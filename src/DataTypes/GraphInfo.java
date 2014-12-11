package DataTypes;
import java.util.*;
import NetworkElements.*;

/**
 * GraphInfo - graph information
 * @author Zhengyang Zuo
 *
 */
public class GraphInfo {
	static public HashMap<Integer, ArrayList<Integer>> graphIP = new HashMap<Integer, ArrayList<Integer>>();
	static public HashMap<Integer, ArrayList<Integer>> graphOpt = new HashMap<Integer, ArrayList<Integer>>();

	static public HashMap<Integer, ArrayList<LSRNIC>> nicsIP = new HashMap<Integer, ArrayList<LSRNIC>>();
	static public HashMap<Integer, ArrayList<LSRNIC>> nicsOpt = new HashMap<Integer, ArrayList<LSRNIC>>();

}