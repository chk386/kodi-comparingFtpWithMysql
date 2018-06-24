Compare files on remote ftp to kodi mysql.

Check the files that exist only on ftp in result.out

# Prerequisites
- JDK9+ (jshell)
- kodi 17.6(20171114-a9a7a20)+

# Installation
git clone https://github.com/chk386/kodi-comparingFtpWithMysql.git
edit kodi.conf
```
remote.ftp.host=192.168.1.1
remote.ftp.port=21
remote.ftp.user=admin
remote.ftp.pass=admin
remote.ftp.path-movies=/User/username/xxx

database.host = 192.168.1.1
database.port = 3306
database.user = root
database.pass = root

movie.extensions=avi,mov,mp4,mkv
```

# Run
- mac, linux : diff.sh
- windows : diff.bat

# Etc
- Windows did not test.
- Errors can be caused by ftp, mysql encoding.
- In that case, please report it.

