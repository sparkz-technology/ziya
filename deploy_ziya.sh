#!/bin/bash

# Define paths
SRC_DIR="/c/Users/athik/Desktop"
FOLDER_NAME="ziya"
ARCHIVE_NAME="ziya.tar.gz"
EXCLUDE_FILE="$SRC_DIR/$FOLDER_NAME/exclude.txt"

# SSH info
DEST_USER="sparkz"
DEST_HOST="135.235.137.144"
DEST_PATH="/home/sparkz"

# Go to source directory
cd "$SRC_DIR" || exit 1

# Create tar.gz excluding unwanted files
tar --exclude-vcs --exclude-ignore="$EXCLUDE_FILE" -czf "$ARCHIVE_NAME" "$FOLDER_NAME"

# Upload the archive via SCP (default port 22)
scp "$ARCHIVE_NAME" "$DEST_USER@$DEST_HOST:$DEST_PATH"

# Optionally extract it on server (you can comment this out if not needed)
ssh "$DEST_USER@$DEST_HOST" "tar -xzf $DEST_PATH/$ARCHIVE_NAME -C $DEST_PATH && rm $DEST_PATH/$ARCHIVE_NAME"

# Cleanup local tar
rm "$ARCHIVE_NAME"

echo "✅ Deployment complete."
