#! /bin/sh
cp jacana-bioqa.jar ~/Vulcan/jacana-bioqa
rsync -aC --exclude .svn resources ~/Vulcan/jacana-bioqa
