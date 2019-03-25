set -e

source .credentials

echo "### 1. Downloading current list..."
az storage blob download -o none -c sermons -n v1/sermons -f sermons.old.json && gzip -f sermons.old.json

echo "### 2. Updating sermon list..."
python3 prepare.py sermons.tsv sermons.old.json.gz sermons.json.gz

echo -n "# Continue with upload (y/n)? "
read confirm
if [ "${confirm}" != "${confirm#[yY]}" ] ;then
    echo "### 3. Uploading new list..."
    az storage blob upload -c sermons -n "v1/archive/sermons-$(date --iso-8601)" -f sermons.json.gz --content-encoding gzip
    az storage blob upload -c sermons -n v1/sermons -f sermons.json.gz --content-encoding gzip
fi
