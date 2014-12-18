package readpeer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLDecoder;

import jclient.JClient;
import jclient.Param;
import kv.Annotation;
import kv.SearchQuery;
import kv.StopWords;
import tool.CandidatesVerifier;
import tool.DataProcessor;
import tool.Messager;
import tool.WordTokenizer;
import lucene.Index;
import lucene.QueryConfig;
import lucene.ReturnValue;

public class Client {

	// flags
	static boolean debug = false;

	static String[] ips;
	static int num_ip;
	static final int MAX_ip = 128;
	static JClient jclient;

	// name for index file
	static String string_index = "string index";
	// name for data file
	static String passage = "data/annotation_dataset_0.txt";

	// to call the functions in master node
	public Param parameter;

	// Top K
	private static int K = 5;

	public Client(String locator) {

		jclient = new JClient(locator);
		// connect all servers
		// content index is the default index
		jclient.connectAllServers(string_index);
	}

	/**
	 * test the insertion
	 * 
	 * @throws IOException
	 * */
	private void testInsertion(String filename, String index_file)
			throws Throwable {

		// initialization for building index
		jclient.initAllServers(Index.STRING_BUILD, index_file);
		// read the data
		BufferedReader buf = new BufferedReader(new InputStreamReader(
				new FileInputStream(filename)));
		String line;
		String id;
		jclient.setMaxVecNum(500);
		while ((line = buf.readLine()) != null) {
			id = line;
			String str = buf.readLine();
			if (str.length() > 255) {
				str = str.substring(0, 255);
			}

			// jclient.addPairs(Long.parseLong(id),0,DataProcessor.getGrams(4,
			// line),Index.STRING_BUILD);

			WordTokenizer wt = new WordTokenizer(new StopWords());
			Annotation anno = wt.getIndexAnno(str);

			if (anno.hasFirst()) {
				jclient.addPairs(Long.parseLong(id), 0, anno.getFirstPart(),
						Index.STRING_BUILD);
			}
			if (anno.hasSecond()) {
				// System.out.println(Long.parseLong(id) + "has second part");
				jclient.addPairs(Long.parseLong(id), 1, anno.getSecondPart(),
						Index.STRING_BUILD);
			}
		}
		jclient.flush();
		// we have to close the writer so that it can
		jclient.closeAllIndexwriters();
		buf.close();
	}

	/**
	 * test query
	 * 
	 * @param String
	 *            for query
	 * @throws Throwable 
	 * */
	private void testDocSearch(String doc, String index_file, int K) throws Throwable{
		jclient.initAllServers(Index.STRING_SEARCH, index_file);
		// initialize the query process

		// long start = System.currentTimeMillis();
		WordTokenizer wt = new WordTokenizer(new StopWords());
		
		BufferedReader buf = new BufferedReader(new InputStreamReader(
				new FileInputStream(doc)));
		String line = new String();
		String qstr = new String();
		while((line = buf.readLine())!=null){
			qstr +=" "+line;
		}
		buf.close();
		
		SearchQuery sQuery = wt.procecssQueryStr(qstr);
		if (sQuery == null) {
			System.out.println(Messager.UNKNOWN_ERROR);
			return;
		}
		// set attributes
		// long preprocess = System.currentTimeMillis()-start;
		STRConfig configs[] = new STRConfig[sQuery.keywordSpace.size()];

		for (int i = 0; i < sQuery.keywordSpace.size(); i++) {
			String word = sQuery.keywordSpace.get(i);
			configs[i] = new STRConfig(0, sQuery, word);
			configs[i].setK(K);
		}

		ReturnValue revalue = jclient.answerDocQuery(configs);
		System.out.println(CandidatesVerifier.verifyCandidates(revalue.sQuery));
	}

	/**
	 * 
	 * test keyword search
	 * 
	 * @param qstr
	 * @param index_file
	 * @param K
	 * @throws Throwable
	 */
	private void testKeywordsSearch(String qstr, String index_file, int K) throws Throwable {
		jclient.initAllServers(Index.STRING_SEARCH, index_file);
		// set the query configurations
		// get grams
		WordTokenizer wt = new WordTokenizer(new StopWords());
		//byte[] encodeArr = qstr.getBytes("UTF-8");  
	    //qstr = new String(encodeArr,"UTF-8");
		qstr = URLDecoder.decode(qstr, "UTF-8");
		String str = wt.processString(qstr);
		String qgrams[] = str.split(" ");
		int num = qgrams.length;
		// set attributes
		STRConfig configs[] = new STRConfig[num];
		for (int i = 0; i < num; i++) {
			configs[i] = new STRConfig(0, qgrams[i]);
			configs[i].setK(K);
		}
		
		ReturnValue revalue = jclient.answerStringQuery(configs);
		int result_num = Math.min(K, revalue.topk_count.size());
		// display the result
		StringBuilder strb = new StringBuilder();

		if (result_num == 0) {
			strb.append("{\"status\":500, \"results\": \"no results\"}");
			System.out.println(strb.toString());
			return;
		} else {
			strb.append("{\"status\":200, \"results\": [");
			strb.append("{\"aid\":" + revalue.topk_index.get(0) + "}");
		}

		for (int i = 1; i < result_num; i++) {
			strb.append(",{\"aid\":" + revalue.topk_index.get(i) + "}");
		}

		strb.append("]}");
		System.out.println(strb.toString());
	}

	public static void main(String[] args) throws Throwable {

		if (args.length < 3) {
			System.out.println(Messager.BAD_REQUEST);
			System.exit(-1);
		}

		debug = false;
		//long start = System.currentTimeMillis();
		Client client = new Client("socket://localhost:1111");

		//client.testInsertion("2", "index");
		//client.testKeywordsSearch("Singapore", "index", 5);
		//client.testDocSearch("copy", "index", K);
		
		
		if (args[0].equals("-index")) {
			//System.out.println("building...");
			try {
				client.testInsertion(args[1], args[2]);
			} catch (Throwable e) {
				System.out.println(Messager.INSERTION_FAIL);
				e.printStackTrace();
				System.exit(-1);
			}
			//System.out.println("building...Done: " + (System.currentTimeMillis() - start));
		}

		else if (args[0].equals("-search")) {
			if (args.length > 4 ) {
				K = Integer.valueOf(args[4]);
			}
			
			try {
				if(args[1].equals("-document")){
					client.testDocSearch(args[2], args[3], K);
					System.out.println("-document");
				}
				
				if(args[1].equals("-keyword")){
					client.testKeywordsSearch(args[2], args[3], K);
				}
				
			} catch (Throwable e) {
				System.out.println(Messager.SEARCH_FAIL);
				e.printStackTrace();
				System.exit(-1);
			}
		} 
		client.jclient.disconnectAllServers();
	}
}

class STRConfig extends QueryConfig {

	public STRConfig(int i, SearchQuery sQuery, String string) {
		super(i, QueryConfig.DOC, sQuery, string);
	}

	public STRConfig(int i, String string) {
		super(i, QueryConfig.STRING, string);
	}

	@Override
	public float calcDistance(long a, long b) {
		return 0;
	}

	@Override
	public int getType() {
		return this.type;
	}

}
