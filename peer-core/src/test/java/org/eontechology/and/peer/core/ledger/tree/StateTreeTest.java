package org.eontechology.and.peer.core.ledger.tree;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.eontechology.and.peer.core.common.TimeProvider;
import org.junit.Before;
import org.junit.Test;

public class StateTreeTest {

    private TreeNodeCollection storage;
    private StateTree<Long> stateTree;

    @Before
    public void setUp() throws Exception {
        storage = new TreeNodeCollection();

        int timestamp = (new TimeProvider()).get();
        stateTree = new StateTree<>(new ValueConverter(timestamp), storage, null);
    }

    @Test
    public void createTest() {

        StateTree<Long> state = stateTree;

        long id = 0b00010000L;
        state = StateTree.createNew(state, id);

        assertNotNull(state.get(id));
        assertTree("L_10000", state);

        id = 0b00100000L;
        state = StateTree.createNew(state, id);

        assertNotNull(state.get(id));
        assertTree("T_(L_100000,L_10000)", state);

        id = 0b00000001L;
        state = StateTree.createNew(state, id);

        assertNotNull(state.get(id));
        assertTree("T_(T_(L_100000,L_10000),L_1)", state);

        id = 0b00000100L;
        state = StateTree.createNew(state, id);

        assertNotNull(state.get(id));
        assertTree("T_(T_(T_(L_100000,L_10000),L_100),L_1)", state);

        id = 0b00000010L;
        state = StateTree.createNew(state, id);

        assertNotNull(state.get(id));
        assertTree("T_(T_(T_(T_(L_100000,L_10000),L_100),L_10),L_1)", state);

        id = 0b00001001L;
        state = StateTree.createNew(state, id);

        assertNotNull(state.get(id));
        assertTree("T_(T_(T_(T_(L_100000,L_10000),L_100),L_10),T_(L_1,L_1001))", state);

        id = 0b00100000L;
        Long value = state.get(id);
        state = StateTree.createNew(state, value);

        assertNotNull(state.get(id));
        assertTree("T_(T_(T_(T_(L_100000,L_10000),L_100),L_10),T_(L_1,L_1001))", state);

        List<Long> list = getList(state);
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

        long[] itemSet = new long[] {0b00010000L, 0b00100000L, 0b00000001L, 0b00000100L, 0b00000010L, 0b00001001L};

        HashMap<Long, Long> dataSet = new HashMap<>();
        StateTree<Long> state = stateTree;

        while (dataSet.size() != itemSet.length) {

            long item = itemSet[r.nextInt(itemSet.length)];

            dataSet.put(item, item);

            state = StateTree.createNew(state, item);
        }

        assertTree("T_(T_(T_(T_(L_100000,L_10000),L_100),L_10),T_(L_1,L_1001))", state);
    }

    private void randomRunner2(Random r) {

        long[] itemSet = new long[] {
                0b01101000,
                0b01011000,
                0b01110100,
                0b01111100,
                0b10110000,
                0b01110010,
                0b00011010,
                0b11000110,
                0b00100101,
                0b01111101
        };

        HashMap<Long, Long> dataSet = new HashMap<>();
        StateTree<Long> state = stateTree;

        while (dataSet.size() != itemSet.length) {

            long item = itemSet[r.nextInt(itemSet.length)];

            dataSet.put(item, item);

            state = StateTree.createNew(state, item);
        }

        assertTree(
                "T_(T_(T_(T_(L_10110000,T_(L_1101000,L_1011000)),T_(L_1110100,L_1111100)),T_(T_(L_1110010,L_11010),L_11000110)),T_(L_100101,L_1111101))",
                state);
    }

    private void randomRunner3(Random r) {

        StateTree<Long> state = stateTree;
        HashMap<Long, Long> dataSet = new HashMap<>();

        while (dataSet.size() != 1000) {

            long id = r.nextLong();

            dataSet.put(id, id);

            state = StateTree.createNew(state, id);
        }

        for (long id : dataSet.keySet()) {
            assertNotNull(state.get(id));
        }

        List<Long> list = getList(state);

        for (int j = 0; j < list.size() - 1; j++) {

            BigInteger i1 = new BigInteger(Long.toHexString(Long.reverse(list.get(j))), 16);
            BigInteger i2 = new BigInteger(Long.toHexString(Long.reverse(list.get(j + 1))), 16);

            assertTrue(i1.compareTo(i2) < 0);
        }
    }

    private List<Long> getList(StateTree<Long> state) {
        ArrayList<Long> list = new ArrayList<>();
        for (Long v : state) {
            list.add(v);
        }
        return list;
    }

    private void assertTree(String value, StateTree state) {
        String t = storage.toString(state.rootNode);
        assertEquals(t, value);
    }

    private static class ValueConverter implements ITreeNodeConverter<Long> {
        private final int timestamp;

        private ValueConverter(int timestamp) {
            this.timestamp = timestamp;
        }

        @Override
        public Long convert(TreeNode treeNode) {
            Map<String, Object> map = treeNode.getValues();
            return (Long) map.get("id");
        }

        @Override
        public TreeNode convert(Long value) {
            HashMap<String, Object> map = new HashMap<>();
            map.put("id", value);
            return new TreeNode(TreeNode.LEAF, timestamp, value, 0, null, null, map);
        }
    }

    private static class TreeNodeCollection implements ITreeNodeCollection {
        private final ConcurrentHashMap<String, TreeNode> map = new ConcurrentHashMap<>();

        @Override
        public TreeNode get(TreeNodeID id) {
            return map.get(id.getKey());
        }

        @Override
        public void add(TreeNode node) {
            map.put(node.getID().getKey(), node);
        }

        public String toString(TreeNode node) {

            if (node.getType() == TreeNode.LEAF) {
                return "L_" + Long.toString(node.getMask(), 2);
            }

            if (node.getType() == 0) {
                return "NULL";
            }

            TreeNode left = get(node.getLeftNodeID());
            TreeNode right = get(node.getRightNodeID());

            return "T_" + "(" + toString(left) + "," + toString(right) + ")";
        }
    }
}
