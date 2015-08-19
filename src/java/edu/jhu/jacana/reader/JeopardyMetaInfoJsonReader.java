/**
 * 
 */
package edu.jhu.jacana.reader;

import java.io.IOException;

import org.json.simple.JSONValue;
import org.json.simple.JSONObject;

import edu.jhu.jacana.util.FileManager;

/**
 * The Jeopardy meta data file looks like:
 * 
 *     "4127.clue_J_6_5": {
 *       "focus_to": 3,
 *       "focus": "Japanese city",
 *       "focus_from": 1,
 *       "clue": "This Japanese city was modeled on \" Cha 'ng - an \" , the T'ang Dynasty 's Chinese capital",
 *       "answer": "Kyoto",
 *       "id": "4127.clue_J_6_5"
 *   },
 * @author Xuchen Yao
 *
 */
public class JeopardyMetaInfoJsonReader {
	
	public static JSONObject id2meta;
	
	public JeopardyMetaInfoJsonReader(String fname) throws IOException {
		String str = FileManager.readFile(fname);
		Object obj = JSONValue.parse(str);
		id2meta = (JSONObject) obj;
		id2meta.get("focus");
	}
	
	private String getByKey(String id, String key) {
		if (id2meta.containsKey(id))
			return (String)((JSONObject)id2meta.get(id)).get(key);
        else
        	return null;
	}
	
	private Integer getIntByKey(String id, String key) {
		if (id2meta.containsKey(id))
			return ((Long)((JSONObject)id2meta.get(id)).get(key)).intValue();
        else
        	return null;
	}
	
	public String getFocus(String id) { return getByKey(id, "focus").replaceAll(":vn", ""); }

	public String getClue(String id) { return getByKey(id, "clue"); }

	public String getAnswer(String id) { return getByKey(id, "answer"); }

	public Integer getFocusFrom(String id) { return getIntByKey(id, "focus_from"); }

	public Integer getFocusTo(String id) { return getIntByKey(id, "focus_to"); }

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		JeopardyMetaInfoJsonReader meta = new JeopardyMetaInfoJsonReader("/tmp/jeopardy.id2meta.json.gz");
		String id = "4127.clue_J_6_4";
		System.out.println(meta.getFocus(id));
		System.out.println(meta.getClue(id));
		System.out.println(meta.getAnswer(id));
		System.out.println(meta.getFocusFrom(id));
		System.out.println(meta.getFocusTo(id));
	}

}
