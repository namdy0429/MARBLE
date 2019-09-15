package marble.boilerplate_miner;

import java.io.IOException;

import com.github.gumtreediff.client.Run;
import com.github.gumtreediff.gen.Generators;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;

public class testAST {

	public static void main(String[] args) throws UnsupportedOperationException, IOException {
		Run.initGenerators();
		String srcFile = "/Users/dayen/eclipse-workspace/api-mining-master/datasets/source/javax_xml_transform/apache.jmeter.XPathUtil.java";
		TreeContext tc1;
		ITree tree1;
		tc1 = Generators.getInstance().getTree(srcFile); 
		tree1 = tc1.getRoot();
		System.out.println("");
	}

}
