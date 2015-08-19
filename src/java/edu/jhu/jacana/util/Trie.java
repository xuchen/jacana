// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

// Benjamin Van Durme, vandurme@cs.jhu.edu, 10 Jun 2011

package edu.jhu.jacana.util;

import java.util.Hashtable;
import java.util.Vector;
import java.io.IOException;
import java.io.BufferedReader;

/**
   @author Benjamin Van Durme

   Simple (not super efficient) Hashtable-based trie for matching phrases in text.
*/
public class Trie {
  TrieNode root;
  boolean caseSensitive;

  /**
     key is the whitespace padded concatenation of the tokens in the interval:
     [start,end)
     from whatever token sequence the Match is based on.
  */
  public class Match {
    public String key;
    public int start;
    public int end;
    public Match (String key, int start, int end) {
	    this.key = key; this.start = start; this.end = end;
    }
  }

  class TrieNode {
    Hashtable<String,TrieNode> next;
    boolean terminal;

    TrieNode (boolean terminal) {
	    this.terminal = terminal;
    }
    void add (String nextKey, boolean terminal) {
	    if (next == null)
        next = new Hashtable();

	    if (! next.containsKey(nextKey))
        next.put(nextKey,new TrieNode(terminal));
	    else // only change the terminal status if going to true
        if (terminal)
          next.get(nextKey).setTerminal(true);
    }
    TrieNode get (String key) {
	    if (next != null)
        return next.get(key);
	    else
        return null;
    }
    boolean terminal () {
	    return terminal;
    }
    void setTerminal (boolean terminal) {
	    this.terminal = terminal;
    }
    boolean containsKey (String key) {
	    if (next == null)
        return false;
	    return next.containsKey(key);
    }
  }

  public void setCaseSensitive (boolean caseSensitive) {
    this.caseSensitive = caseSensitive;
  }

  /**
     File should be of the form:
     token( token)* (TAB .*)*

     That is, one or more tokens per line, making up a phrase, optionally
     followed by additional columns which will be ignored here.
  */
  public void loadPhrases (String filename) throws IOException {
    root = new TrieNode(false);
    TrieNode node;
    String line;
    String[] tokens;
    String[] columns;
    BufferedReader reader = FileManager.getReader(filename);
    while ((line = reader.readLine()) != null) {
	    node = root;
	    if (! caseSensitive)
        line = line.toLowerCase();
	    columns = line.split("\t+");
	    tokens = columns[0].split("\\s+");
	    if (tokens.length > 0) {
        for (int i = 0; i < tokens.length - 1; i++) {
          node.add(tokens[i],false);
          node = node.get(tokens[i]);
        }
        node.add(tokens[tokens.length-1],true);
	    }
    }
    reader.close();
  }

  Trie.Match[] matchArr = new Trie.Match[0];

  public Trie.Match[] matches (String[] tokens) {
    TrieNode node;
    Vector<Trie.Match> results = new Vector();
    String phrase;
    String token;
    int j;
    for (int i = 0; i < tokens.length; i++) {
	    node = root;
	    j = i;
	    phrase = "";
	    token = caseSensitive ? tokens[j] : tokens[j].toLowerCase();
	    while (j < tokens.length && node.containsKey(token)) {
        node = node.get(token);

        if (j != i)
          phrase += " " + token;
        else
          phrase = token;

        if (node.terminal())
          results.add(new Match(phrase,i,j+1));

        j++;
        if (j < tokens.length)
          token = caseSensitive ? tokens[j] : tokens[j].toLowerCase();
	    }
    }
    return results.toArray(matchArr);
  }

  public static void main (String[] args) throws Exception {
    Trie trie = new Trie();
    trie.setCaseSensitive(false);
    trie.loadPhrases(args[0]);
    BufferedReader reader = FileManager.getReader(args[1]);
    String line;
    Trie.Match[] matches;
    while ((line = reader.readLine()) != null) {
	    System.out.println(line);
	    System.out.print("=> ");
	    matches = trie.matches(line.split("\\s+"));
	    for (Trie.Match match : matches)
        System.out.print("[" + match.key + "]:" + match.start + ":" + match.end + " ");
	    System.out.println();
    }
  }
}
