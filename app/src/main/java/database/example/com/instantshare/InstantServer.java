package database.example.com.instantshare;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.TimeZone;

/**
 * * ============================================================================
 * * Copyright (C) 2018 W3 Engineers Ltd - All Rights Reserved.
 * * Unauthorized copying of this file, via any medium is strictly prohibited
 * * Proprietary and confidential
 * * ----------------------------------------------------------------------------
 * * Created by: Mimo Saha on [10-Jul-2018 at 12:48 PM].
 * * Email: mimosaha@w3engineers.com
 * * ----------------------------------------------------------------------------
 * * Project: DataTransferServer.
 * * Code Responsibility: <Purpose of code>
 * * ----------------------------------------------------------------------------
 * * Edited by :
 * * --> <First Editor> on [10-Jul-2018 at 12:48 PM].
 * * --> <Second Editor> on [10-Jul-2018 at 12:48 PM].
 * * ----------------------------------------------------------------------------
 * * Reviewed by :
 * * --> <First Reviewer> on [10-Jul-2018 at 12:48 PM].
 * * --> <Second Reviewer> on [10-Jul-2018 at 12:48 PM].
 * * ============================================================================
 **/
public class InstantServer {

    private int port;
    private String HTTP_OK = "200 OK",
            HTTP_PARTIALCONTENT = "206 Partial Content",
            HTTP_RANGE_NOT_SATISFIABLE = "416 Requested Range Not Satisfiable",
            HTTP_NOTMODIFIED = "304 Not Modified",
            filePath;

    String METHOD = "method";
    String URI = "uri";

    private String MIME_DEFAULT_BINARY = "application/octet-stream",
            MIME_PLAINTEXT = "text/plain",
            MIME_HTML = "text/html";

    private PercentCallback percentCallback;

    InstantServer(int port, String filePath) {
        this.port = port;
        this.filePath = filePath;
        startServer();
    }

    public InstantServer setPercentCallback(PercentCallback percentCallback) {
        this.percentCallback = percentCallback;
        return this;
    }

    /**
     * Purpose of this callback is
     * How amount of data has been passed
     */
    public interface PercentCallback {
        void showPercent(int percent);
    }

    private ServerSocket serverSocket;
    private Thread thread;

    /**
     * The purpose of this API is socket initialize
     * and ready for accepting any type of request
     * which through using POST or GET method
     * then start this server
     */
    private void startServer(){
        try {
            serverSocket = new ServerSocket(port);
            thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        while (true) {
                            Socket socket = serverSocket.accept();
                            new HTTPRequestSession(socket);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });

            thread.setDaemon(true);
            thread.start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * When our purpose is done then we stop this server
     * and this time close our server socket
     */
    public void stopServer() {
        try {
            serverSocket.close();
            thread.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class HTTPRequestSession implements Runnable {

        private Socket socket;
        private Properties methods, header, parameters;
        private SimpleDateFormat simpleDateFormat;

        HTTPRequestSession(Socket socket) {
            this.socket = socket;
            Thread thread = new Thread(this);
            thread.setDaemon(true);
            thread.start();
        }

        private void reset() {
            if (methods != null)
                methods.clear();
            if (header != null)
                header.clear();
            if (parameters != null)
                parameters.clear();
        }

        private void init() {

            methods = new Properties();
            header = new Properties();
            parameters = new Properties();

            simpleDateFormat = new SimpleDateFormat("E, d MMM yyyy HH:mm:ss 'GMT'", Locale.US);
            simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        }

        @Override
        public void run() {
            try {
                InputStream inputStream = socket.getInputStream();
                if (inputStream == null)
                    return;

                int bufferSize = 8192;
                byte[] buffer = new byte[bufferSize];
                int reqLength = inputStream.read(buffer, 0, bufferSize);
                if (reqLength <= 0) return;

                reset();
                init();

                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(buffer, 0, reqLength);
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(byteArrayInputStream));

                decodeHeaderData(bufferedReader);

                Response response = serveData(methods.getProperty(URI), header);

                if (response != null) {
                    sendResponse(response);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void sendResponse(Response response) {

            String status = response.status;
            String mimeType = response.mimeType;
            InputStream inputStream = response.inputStream;

            int theBufferSize = 16 * 1024;

            try {
                if (status == null)
                    throw new Error("sendResponse(): MessageBase can't be null.");

                OutputStream out = socket.getOutputStream();
                PrintWriter pw = new PrintWriter(out);
                pw.print("HTTP/1.0 " + status + " \r\n");

                if (mimeType != null)
                    pw.print("Content-Type: " + mimeType + "\r\n");

                if (header == null || header.getProperty("Date") == null)
                    pw.print("Date: " + simpleDateFormat.format(new Date()) + "\r\n");

                if (header != null) {
                    Enumeration e = header.keys();
                    while (e.hasMoreElements()) {
                        String key = (String) e.nextElement();
                        String value = header.getProperty(key);
                        pw.print(key + ": " + value + "\r\n");
                    }
                }

                pw.print("\r\n");
                pw.flush();

                if (inputStream != null) {
                    int pending = inputStream.available();    // This is to support partial sends, see serveFile()
                    byte[] buff = new byte[theBufferSize];
                    long lengthOfFile = inputStream.available(), total = 0;
                    while (pending > 0) {
                        int read = inputStream.read(buff, 0, ((pending > theBufferSize) ? theBufferSize : pending));
                        if (read <= 0) break;
                        out.write(buff, 0, read);
                        pending -= read;

                        total += read;
                        int responsePercentage = (int) ((total * 100) / lengthOfFile);
                        getResponseProgress(responsePercentage);
                    }
                }
                out.flush();
                out.close();
                if (inputStream != null)
                    inputStream.close();

            } catch (IOException ioe) {
                try {
                    socket.close();
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }

        private void decodeHeaderData(BufferedReader bufferedReader) {

            try {
                String readLine = bufferedReader.readLine();
                StringTokenizer stringTokenizer = new StringTokenizer(readLine);

                if (!stringTokenizer.hasMoreTokens())
                    return;

                methods.setProperty(METHOD, stringTokenizer.nextToken());
                methods.setProperty(URI, stringTokenizer.nextToken());

                String line = bufferedReader.readLine();
                while (line != null && line.trim().length() > 0) {
                    int p = line.indexOf(':');
                    if (p >= 0)
                        header.put(line.substring(0, p).trim().toLowerCase(),
                                line.substring(p + 1).trim());
                    line = bufferedReader.readLine();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void getResponseProgress(int responsePercentage) {
        if (percentCallback != null) {
            percentCallback.showPercent(responsePercentage);
        }
    }

    private class Response {
        String status, mimeType;
        InputStream inputStream;
        Properties header = new Properties();

        void addHeader(String key, String value) {
            header.put(key, value);
        }

        Response(String status, String mimeType, InputStream inputStream) {
            this.status = status;
            this.mimeType = mimeType;
            this.inputStream = inputStream;
        }

        Response(String status, String mimeType, String txt) {
            this.status = status;
            this.mimeType = mimeType;
            try {
                this.inputStream = new ByteArrayInputStream(txt.getBytes("UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
    }

    private Response serveData(String uri, Properties method) {
        return serveFile(uri, method);
    }

    private Response prepareFile(Properties header) {
        Response response = null;
        String mime = MIME_DEFAULT_BINARY;

        try {
            File f = new File(filePath);

            // Calculate etag
            String etag = Integer.toHexString((f.getAbsolutePath() + f.lastModified() + "" + f.length()).hashCode());

            // Support (simple) skipping:
            long startFrom = 0;
            long endAt = -1;
            String range = header.getProperty("range");
            if (range != null) {
                if (range.startsWith("bytes=")) {
                    range = range.substring("bytes=".length());
                    int minus = range.indexOf('-');
                    try {
                        if (minus > 0) {
                            startFrom = Long.parseLong(range.substring(0, minus));
                            endAt = Long.parseLong(range.substring(minus + 1));
                        }
                    } catch (NumberFormatException nfe) {
                    }
                }
            }

            // Change return code and add Content-Range header when skipping is requested
            long fileLen = f.length();
            if (range != null && startFrom >= 0) {
                if (startFrom >= fileLen) {
                    response = new Response(HTTP_RANGE_NOT_SATISFIABLE, MIME_PLAINTEXT, "");
                    response.addHeader("Content-Range", "bytes 0-0/" + fileLen);
                    response.addHeader("ETag", etag);
                } else {
                    if (endAt < 0)
                        endAt = fileLen - 1;
                    long newLen = endAt - startFrom + 1;
                    if (newLen < 0) newLen = 0;

                    final long dataLen = newLen;
                    FileInputStream fis = new FileInputStream(f) {
                        public int available() throws IOException {
                            return (int) dataLen;
                        }
                    };
                    fis.skip(startFrom);

                    response = new Response(HTTP_PARTIALCONTENT, mime, fis);
                    response.addHeader("Content-Length", "" + dataLen);
                    response.addHeader("Content-Range", "bytes " + startFrom + "-" + endAt + "/" + fileLen);
                    response.addHeader("ETag", etag);
                }
            } else {
                if (etag.equals(header.getProperty("if-none-match")))
                    response = new Response(HTTP_NOTMODIFIED, mime, "");
                else {
                    response = new Response(HTTP_OK, mime, new FileInputStream(f));
                    response.addHeader("Content-Length", "" + fileLen);
                    response.addHeader("ETag", etag);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (response != null) {
            response.addHeader("Accept-Ranges", "bytes");
        }

        return response;
    }

    private Response serveFile(String uri, Properties header) {

        Response response = null;
        try {

            if (uri.contains("DOWNLOAD_CONTENT")) {
                response = prepareFile(header);
            } else {
                InputStream inputStream = WebUpdater.getWebUpdater().getWebFile(filePath);
                response = new Response(HTTP_OK, MIME_HTML, inputStream);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return response;
    }

}
