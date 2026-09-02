#!/usr/bin/env sh
set -eu

if [ -d .git ]; then
  echo "This folder is already a Git repository."
  exit 0
fi

git init
git add .
git commit -m "chore: bootstrap Travel Companion Android"

echo
echo "Git repository initialized."
echo "Next: create a remote repository and add it with:"
echo "  git remote add origin <url>"
