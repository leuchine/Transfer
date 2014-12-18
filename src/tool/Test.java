package tool;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;

import kv.Annotation;
import kv.SearchQuery;
import kv.StopWords;

public class Test {
	public static void main(String[] args) throws IOException {
		WordTokenizer token = new WordTokenizer(new StopWords());
		WordTokenizer token3 = new WordTokenizer(new StopWords());
		System.out
				.println(token3
						.processString(" Then a database is a set of d-dimensional points, where d is the dimensionality. The notation used in this paper is summarized in Table 1 for easy reference.  2.1  The K -N -Match Problem For ease of illustration, we start with the simplest form of the"));

		Annotation after2 = token
				.getIndexAnno(" Then a database is a set of d-dimensional points, where d is the dimensionality. The notation used in this paper is summarized in Table 1 for easy reference.  2.1  The K -N -Match Problem For ease of illustration, we start with the simplest form of the");
		Iterator<Integer> it = token.segPos.iterator();
		while (it.hasNext()) {
			System.out.print(it.next() + " ");
		}
		System.out.println();
		System.out.println(after2.getFirstPart());
		System.out.println(after2.getSecondPart());

		WordTokenizer token2 = new WordTokenizer(new StopWords());
		BufferedReader buf = new BufferedReader(new InputStreamReader(
				new FileInputStream(System.getProperty("user.dir")
						+ "/test.txt")));
		String line = new String();
		String qstr = new String();
		while ((line = buf.readLine()) != null) {
			qstr += " " + line;
		}
		buf.close();

		SearchQuery query = token2.procecssQueryStr(qstr);
		// System.out.println(token
		// .processString(qstr));
	}
}
