import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

String KODI_CONF = "kodi.conf";
List ftpFiles = new ArrayList<String>();
List dbFiles = new ArrayList<String>();
Properties properties = new Properties();

void loadKodiConfig() throws IOException {
    try(FileReader fr = new FileReader(KODI_CONF)) {
        properties.load(fr);
    }
}

void readFtpFiles(Properties properties) throws IOException {
    FTPClient ftpClient = new FTPClient();
    String host = (String)properties.get("remote.ftp.host");
    int port = Integer.parseInt((String)properties.get("remote.ftp.port"));
    String user = (String)properties.get("remote.ftp.user");
    String pass = (String)properties.get("remote.ftp.pass");
    String moviePath = (String)properties.get("remote.ftp.path-movies");
    String[] extensions = ((String)properties.get("movie.extensions")).split(",");

    ftpClient.enterLocalPassiveMode();
    ftpClient.connect(host, port);
    ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
    ftpClient.setAutodetectUTF8(true);

    if(!ftpClient.login(user, pass)) {
        throw new IOException("login failed!");
    }

    System.out.println(String.format("connect to ftp (movie path) : %s", moviePath));
    ftpClient.changeWorkingDirectory(moviePath);

    findFilesByRecursive(ftpClient, moviePath, extensions);
    ftpClient.disconnect();
}

@SuppressWarnings("unchecked")
void findFilesByRecursive(FTPClient ftpClient, String path, String[] extensions) throws IOException {
    for(FTPFile ftpFile : ftpClient.listFiles()) {
        String fullPath = path + File.separator + ftpFile.getName();

        if(ftpFile.isDirectory()) {
            ftpClient.changeWorkingDirectory(fullPath);
            findFilesByRecursive(ftpClient, fullPath, extensions);
        }else if(Arrays.stream(extensions).anyMatch(ext -> ftpFile.getName().endsWith(ext))) {
            ftpFiles.add(new String(fullPath.getBytes("ISO-8859-1"), "UTF-8"));
        }
    }
}

@SuppressWarnings("unchecked")
void readDatabase() throws Exception {
    Class.forName("com.mysql.cj.jdbc.Driver");
    String hostname = (String)properties.get("database.host");
    String port = (String)properties.get("database.port");
    String user = (String)properties.get("database.user");
    String pass = (String)properties.get("database.pass");
    String url = String.format("jdbc:mysql://%s:%s/MyVideos107?useSSL=false&characterEncoding=UTF-8", hostname, port);
    String moviePath = (String)properties.get("remote.ftp.path-movies");

    Connection conn = DriverManager.getConnection(url, user, pass);
    PreparedStatement ps = conn.prepareStatement("select c22 from movie");

    ResultSet rs = ps.executeQuery();

    while(rs.next()) {
        String filePath = rs.getString("c22");
        String STACK_PREFIX = "stack://";
        if(filePath.startsWith(STACK_PREFIX)) {
            String tmp = filePath.substring(STACK_PREFIX.length());
            String[] ary = tmp.split(" , ");
            for(String stackFileName : ary) {
                dbFiles.add(stackFileName.substring(stackFileName.indexOf(moviePath), stackFileName.length()));
            }

        }else {
            dbFiles.add(filePath.substring(filePath.indexOf(moviePath), filePath.length()));
        }
    }

    conn.close();
}

@SuppressWarnings("unchecked")
void diffData() throws IOException {
    dbFiles.forEach(dbFile -> {
        ftpFiles.remove(dbFile);
    });

    if(ftpFiles.isEmpty()) {
        System.out.println("All files are synchronized!");
    }else {
        try(Writer writer = new FileWriter("result.out")) {
            for(Object ftpFile : ftpFiles) {
                writer.write(ftpFile + "\n");
            }
        }

        System.out.println("\nSee \"result.out\"");
    }
}

try {
    loadKodiConfig();
    readFtpFiles(properties);
    readDatabase();

    diffData();

}catch(Exception e) {
    e.printStackTrace();
}

/exit

