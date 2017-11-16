package com.exscudo.peer.store.sqlite.merkle;

import static org.junit.Assert.*;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.Before;
import org.junit.Test;

import com.exscudo.peer.store.sqlite.ISegment;

public class TreeTest {

	private Storage storage;

	@Before
	public void setUp() throws Exception {
		storage = new Storage();
	}

	@Test
	public void createTest() {

		Tree tree = new Tree(storage, null);

		long id = 0b00010000L;
		HashMap<String, Object> data = new HashMap<>();
		data.put("id", id);
		tree.put(id, data);

		assertNotNull(tree.get(id));
		assertTree("L_10000", tree);

		id = 0b00100000L;
		data = new HashMap<>();
		data.put("id", id);
		tree.put(id, data);

		assertNotNull(tree.get(id));
		assertTree("T_110000(L_100000,L_10000)", tree);

		id = 0b00000001L;
		data = new HashMap<>();
		data.put("id", id);
		tree.put(id, data);

		assertNotNull(tree.get(id));
		assertTree("T_110001(T_110000(L_100000,L_10000),L_1)", tree);

		id = 0b00000100L;
		data = new HashMap<>();
		data.put("id", id);
		tree.put(id, data);

		assertNotNull(tree.get(id));
		assertTree("T_110101(T_110100(T_110000(L_100000,L_10000),L_100),L_1)", tree);

		id = 0b00000010L;
		data = new HashMap<>();
		data.put("id", id);
		tree.put(id, data);

		assertNotNull(tree.get(id));
		assertTree("T_110111(T_110110(T_110100(T_110000(L_100000,L_10000),L_100),L_10),L_1)", tree);

		id = 0b00001001L;
		data = new HashMap<>();
		data.put("id", id);
		tree.put(id, data);

		assertNotNull(tree.get(id));
		assertTree("T_111110(T_110110(T_110100(T_110000(L_100000,L_10000),L_100),L_10),T_1000(L_1,L_1001))", tree);

		id = 0b00100000L;
		data = new HashMap<>();
		data.put("id", id + 1);
		tree.put(id, data);

		assertNotNull(tree.get(id));
		assertTree("T_111110(T_110110(T_110100(T_110000(L_100000,L_10000),L_100),L_10),T_1000(L_1,L_1001))", tree);

		List<Long> list = getList(tree.getRootNode(), storage);
		for (int i = 0; i < list.size() - 1; i++) {

			BigInteger i1 = new BigInteger(Long.toHexString(Long.reverse(list.get(i))), 16);
			BigInteger i2 = new BigInteger(Long.toHexString(Long.reverse(list.get(i + 1))), 16);

			assertTrue(i1.compareTo(i2) < 0);
		}
	}

	@Test
	public void randomRunnerTest() {

		for (int i = 0; i < 10; i++) {
			randomRunner(new Random(i));
			randomRunner2(new Random(i));
			randomRunner3(new Random(i));
		}

	}

	private void randomRunner(Random r) {

		long[] itemSet = new long[] { 0b00010000L, 0b00100000L, 0b00000001L, 0b00000100L, 0b00000010L, 0b00001001L };

		HashMap<Long, HashMap<String, Object>> dataSet = new HashMap<>();

		Tree tree = new Tree(storage, null);

		while (dataSet.size() != itemSet.length) {

			long item = itemSet[r.nextInt(itemSet.length)];
			HashMap<String, Object> data = new HashMap<>();
			data.put("id", item);

			dataSet.put(item, data);

			tree.put(item, data);
		}

		assertTree("T_111110(T_110110(T_110100(T_110000(L_100000,L_10000),L_100),L_10),T_1000(L_1,L_1001))", tree);
	}

	private void randomRunner2(Random r) {

		long[] itemSet = new long[] { 0b01101000, 0b01011000, 0b01110100, 0b01111100, 0b10110000, 0b01110010,
				0b00011010, 0b11000110, 0b00100101, 0b01111101 };

		HashMap<Long, HashMap<String, Object>> dataSet = new HashMap<>();

		Tree tree = new Tree(storage, null);

		while (dataSet.size() != itemSet.length) {

			long item = itemSet[r.nextInt(itemSet.length)];
			HashMap<String, Object> data = new HashMap<>();
			data.put("id", item);

			dataSet.put(item, data);

			tree.put(item, data);
		}

		assertTree(
				"T_1111110(T_100110(T_10001000(T_10000000(L_10110000,T_110000(L_1101000,L_1011000)),T_1000(L_1110100,L_1111100)),T_10101110(T_1101000(L_1110010,L_11010),L_11000110)),T_1011000(L_100101,L_1111101))",
				tree);
	}

	private void randomRunner3(Random r) {

		Tree tree = new Tree(storage, null);
		HashMap<Long, HashMap<String, Object>> dataSet = new HashMap<>();

		while (dataSet.size() != 1000) {

			long id = r.nextLong();
			HashMap<String, Object> data = new HashMap<>();
			data.put("id", id);
			dataSet.put(id, data);

			tree.put(id, data);

		}

		for (long id : dataSet.keySet()) {
			assertNotNull(tree.get(id));
		}

		List<Long> list = getList(tree.getRootNode(), storage);

		for (int j = 0; j < list.size() - 1; j++) {

			BigInteger i1 = new BigInteger(Long.toHexString(Long.reverse(list.get(j))), 16);
			BigInteger i2 = new BigInteger(Long.toHexString(Long.reverse(list.get(j + 1))), 16);

			assertTrue(i1.compareTo(i2) < 0);
		}

	}

	public List<Long> getList(TreeNode node, Storage storage) {
		ArrayList<Long> list = new ArrayList<>();
		addToList(node, list, storage);
		return list;
	}

	private void addToList(TreeNode root, List<Long> list, Storage storage) {
		if (root.getType() == TreeNode.LEAF) {
			list.add(root.getId());
			return;
		}

		String targetLeft = root.getValue("left").toString();
		String targetRight = root.getValue("right").toString();

		TreeNode left = storage.get(targetLeft);
		TreeNode right = storage.get(targetRight);

		addToList(left, list, storage);
		addToList(right, list, storage);
	}

	private void assertTree(String value, Tree tree) {
		String t = format(tree.getRootNode(), storage);
		assertEquals(t, value);
	}

	private String format(TreeNode node, Storage storage) {

		if (node.getType() == TreeNode.LEAF) {
			return "L_" + Long.toString(node.getId(), 2);
		}

		if (node.getType() == 0) {
			return "NULL";
		}

		String targetLeft = node.getValue("left").toString();
		String targetRight = node.getValue("right").toString();

		TreeNode left = storage.get(targetLeft);
		TreeNode right = storage.get(targetRight);

		return "T_" + Long.toString(node.getId(), 2) + "(" + format(left, storage) + "," + format(right, storage) + ")";
	}

	class Storage implements ISegment<String, TreeNode> {
		private final ConcurrentHashMap<String, TreeNode> map = new ConcurrentHashMap<>();

		@Override
		public void put(String key, TreeNode value) {
			map.put(key, value);
		}

		@Override
		public TreeNode get(String key) {
			return map.get(key);
		}

		@Override
		public void remove(String key) {
			map.remove(key);
		}

	}

}
