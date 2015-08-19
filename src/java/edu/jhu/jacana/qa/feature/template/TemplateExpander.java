/**
 * 
 */
package edu.jhu.jacana.qa.feature.template;

/**
 * @author Xuchen Yao
 *
 */
public class TemplateExpander {
	
	public static String BOS = "<s>", EOS = "</s>";
		
	public static final int[][] LEFT_RIGHT_BY_ZERO = new int[][] {
		{0},
	};
	
	public static final int[][] LEFT_RIGHT_BY_ONE = new int[][] {
		{0},
		{-1},
		{1},
		{-1,0},
		{0,1}
	};
	
	public static final int[][] LEFT_RIGHT_BY_TWO = new int[][] {
		{0},
		{-1},
		{1},
		{-2},
		{2},
		{-1,0},
		{0,1},
		{-2,-1},
		{1,2},
		{-2,-1,0},
		{0,1,2},
		//{-1,0,1}
	};

	public static String[] expand(String[] features, String name, int[][] template) {
		if (template == null) return features;
		String[] expanded = new String[features.length];
		for (int i=0; i<features.length; i++) {
			StringBuilder sb = new StringBuilder();
			for (int j=0; j<template.length; j++) {
				// in a featuer such as "pos[-1]|pos[0]=dt|nn"
				// sbLeft is "pos[-1]pos[0]", sbRight is "dt|nn"
				StringBuilder sbLeft = new StringBuilder();
				StringBuilder sbRight = new StringBuilder();
				for (int k=0; k<template[j].length; k++) {
					int offset = template[j][k];
					sbLeft.append(name+"["+offset+"]");
					if (i+offset < 0)
						sbRight.append(BOS);
					else if (i+offset >= features.length)
						sbRight.append(EOS);
					else
						sbRight.append(features[i+offset]);
					if (k != template[j].length -1) {
						sbLeft.append("|");
						sbRight.append("|");
					}
				}
				sb.append(sbLeft+"="+sbRight);
				if (j != template.length-1)
					sb.append("\t");
			}
			expanded[i] = sb.toString().trim();
		}
		return expanded;
	}
}
