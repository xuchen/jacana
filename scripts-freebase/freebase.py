#! /usr/bin/env python

import json, os
import urllib

api_key = open(os.path.expanduser("~") + "/.api_key").read()

topic_service_url = 'https://www.googleapis.com/freebase/v1/topic'
topic_params = {
  'key': api_key,
  'filter': 'all'
}

def download(topic_id):
    # topic_id = '/m/0d6lp' or '/en/justin_bieber'
    url = topic_service_url + topic_id + '?' + urllib.urlencode(topic_params)
    # topic = json.loads(urllib.urlopen(url).read())
    string = urllib.urlopen(url).read()
    success = (string.find('"error":') == -1 and  string.find('"code":') == -1)
    return (success, string)


search_service_url = 'https://www.googleapis.com/freebase/v1/search'
def search(query):
    params = { 'query': query, 'key': api_key}
    url = search_service_url + '?' + urllib.urlencode(params)
    # response = json.loads(urllib.urlopen(url).read())
    response = urllib.urlopen(url).read()
    return response
    # for result in response['result']:
    #     print result['name'] + ' (' + str(result['score']) + ')'


# print download('/en/justin_bieber')
print download('/m/04jb3xf')
print search('cleveland ohio')
