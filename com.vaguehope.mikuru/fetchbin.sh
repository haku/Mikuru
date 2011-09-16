#/bin/bash
wget http://adqmisc.googlecode.com/svn/trunk/androidutils/rsync/rsync-3.0.6-arm-softfloat-linux-gnueabi.gz
zcat rsync-3.0.6-arm-softfloat-linux-gnueabi.gz > assets/rsync
rm rsync-3.0.6-arm-softfloat-linux-gnueabi.gz
