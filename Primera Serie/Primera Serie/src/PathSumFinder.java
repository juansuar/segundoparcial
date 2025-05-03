import java.util.ArrayList;
import java.util.List;

public class PathSumFinder {

    static class TreeNode {
        int val;
        TreeNode left;
        TreeNode right;

        TreeNode(int val) {
            this.val = val;
        }
    }

    public List<List<Integer>> pathSum(TreeNode root, int targetSum) {
        List<List<Integer>> result = new ArrayList<>();
        List<Integer> currentPath = new ArrayList<>();
        dfs(root, targetSum, currentPath, result);
        return result;
    }

    private void dfs(TreeNode node, int remainingSum, List<Integer> currentPath, List<List<Integer>> result) {
        if (node == null) {
            return;
        }

        currentPath.add(node.val);

        if (node.left == null && node.right == null && remainingSum == node.val) {
            result.add(new ArrayList<>(currentPath));
        } else {
            dfs(node.left, remainingSum - node.val, currentPath, result);
            dfs(node.right, remainingSum - node.val, currentPath, result);
        }

        currentPath.remove(currentPath.size() - 1);
    }

    public static void main(String[] args) {
        PathSumFinder finder = new PathSumFinder();

        TreeNode root = new TreeNode(5);
        root.left = new TreeNode(4);
        root.right = new TreeNode(8);
        root.left.left = new TreeNode(11);
        root.left.left.left = new TreeNode(7);
        root.left.left.right = new TreeNode(2);
        root.right.left = new TreeNode(13);
        root.right.right = new TreeNode(4);
        root.right.right.left = new TreeNode(5);
        root.right.right.right = new TreeNode(1);

        int targetSum = 22;

        List<List<Integer>> paths = finder.pathSum(root, targetSum);

        System.out.println("Rutas encontradas:");
        for (List<Integer> path : paths) {
            System.out.println(path);
        }
    }
}