/**
 * 
 */
package edu.jhu.jacana.test;

import approxlib.distance.EditDist;
import approxlib.tree.LblTree;

/**
 * @author Xuchen Yao
 *
 */
public class TestTreeDist {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		String[] treeList = {
//				"{root{f{d{a}{c{b}}}{e}}}", "{root{f{c{d{a}{b}}}{e}}}", 			//del(3);ins(4,6,1,1)
//				"{root{f{d{a}{c{b}}}{g}{e}}}", "{root{f{c{d{a}{b}}{g}}{e}}}",		//del(3);ins(5,7,1,2)
//				"{root{a{b}{c}}}", "{root{a{c}{b}}}",								//ins(1,3,0,0);del(2)
//				"{root{a{b}{c}}}", "{root{d{e}{f}}}",								//ren(1,1);ren(2,2);ren(3,3)
//				"{root{a{b{d}{e}{f{g}{h}}}{c}}}", "{root{f{g}{h}}}",				//del(1);del(2);del(6);del(7);del(8)
//				"{root{d}}", "{root{g{h}}}",										//ren(1,1);ins(2,2,1,1)
//				"{root{a{b}{c}}}", "{root{d{c}{e}}}",								//del(1);ins(2,3,0,0);ren(3,3)
//				"{root{a{b{c}{d}}{e{f}{g}}}}", "{root{h{i{j}{k{b{c}{d}}}}{l}{m{a{e{f}{g}}{g}}}}}",		//ins(1,0,0,0);ins(5,7,1,1);ins(6,7,1,1);ins(7,7,1,1);ins(8,7,1,1);del(4);ins(10,6,0,2);del(6);ins(11,7,1,2);ins(12,7,2,2);ins(13,7,2,2);ren(7,14)
//				"{root{a{b{c}{d}}}}", "{root{i{j}{b{c}{d}}}}",						//ins(1,4,0,0);ren(4,5)
//				"{root{a{b}}}", "{root{i{j}{b}}}",									//ins(1,2,0,0);ren(2,3)
//				"{root{a{b{c}{d}}}}", "{root{i{j}{k{b{c}{d}}}}}",					//ins(1,5,0,0);ren(4,5);ins(6,5,1,1)
//				"{root{b{c}{d}}}", "{root{i{j}{k{b{c}{d}}}}}",						//ins(1,0,0,0);ins(5,4,0,0);ins(6,0,0,0)
//				"{root{d{a}{c{b}}}}", "{root{d{a}{b}}}",							//del(3)
//				"{root{a{b{d{f}{g}}{e}}{c}}}", "{root{a{b}{c{k{m}}{l}}{h{i}{j}}}}"
//				"{root{a{b}{c}}}", "{root{a{b}{c}}}",
				"{root{a{b}{c}}}", "{root{a{b}{c}{d{e{f}}}}}",
				};
		for (int i=0; i<treeList.length/2; i++) {
			LblTree t1 = LblTree.fromString(treeList[2*i]);
			LblTree t2 = LblTree.fromString(treeList[2*i+1]);
			t1.prettyPrint();
			t2.prettyPrint();
			
			EditDist dis2 = new EditDist(1,1,3,false);
			System.out.println(dis2.treeDist(t1, t2));			
			dis2.printForestDist();
			dis2.printTreeDist();
			dis2.printBackPointer();
			dis2.printEditMatrix();
			System.out.println(dis2.printEditScript());
			System.out.println(dis2.printHumaneEditScript());
			//System.out.println(dis2.getCompactEditList());
			System.out.println(dis2.printCompactEditScript());
		}

	}

}
