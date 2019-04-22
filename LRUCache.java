package trabalhin;
import java.io.*;
import java.net.*;
import java.util.*;
public class LRUCache<String, File> {

    // Define Node with pointers to the previous and next items and a key, value pair
    class Node<T, U> {
        Node<T, U> previous;
        Node<T, U> next;
        T key;
        U value;

        public Node(Node<T, U> previous, Node<T, U> next, T key, U value){
            this.previous = previous;
            this.next = next;
            this.key = key;
            this.value = value;
        }
    }

    private HashMap<String, Node<String, File>> cache;
    private Node<String, File> leastRecentlyUsed;
    private Node<String, File> mostRecentlyUsed;
    private int maxSize;
    private int currentSize;

    public LRUCache(int maxSize){
        this.maxSize = maxSize;
        this.currentSize = 0;
        leastRecentlyUsed = new Node<String, File>(null, null, null, null);
        mostRecentlyUsed = leastRecentlyUsed;
        cache = new HashMap<String, Node<String, File>>();
    }

    public File get(String key){
        Node<String, File> tempNode = cache.get(key);
        if (tempNode == null){
            return null;
        }
        // If MRU leave the list as it is
        else if (tempNode.key == mostRecentlyUsed.key){
            return mostRecentlyUsed.value;
        }

        // Get the next and previous nodes
        Node<String, File> nextNode = tempNode.next;
        Node<String, File> previousNode = tempNode.previous;

        // If at the left-most, we update LRU 
        if (tempNode.key == leastRecentlyUsed.key){
            nextNode.previous = null;
            leastRecentlyUsed = nextNode;
        }

        // If we are in the middle, we need to update the items before and after our item
        else if (tempNode.key != mostRecentlyUsed.key){
            previousNode.next = nextNode;
            nextNode.previous = previousNode;
        }

        // Finally move our item to the MRU
        tempNode.previous = mostRecentlyUsed;
        mostRecentlyUsed.next = tempNode;
        mostRecentlyUsed = tempNode;
        mostRecentlyUsed.next = null;

        return tempNode.value;

    }

    public void put(String key, File value){
        if (cache.containsKey(key)){
            return;
        }

        // Put the new node at the right-most end of the linked-list
        Node<String, File> myNode = new Node<String, File>(mostRecentlyUsed, null, key, value);
        mostRecentlyUsed.next = myNode;
        cache.put(key, myNode);
        mostRecentlyUsed = myNode;

        // Delete the left-most entry and update the LRU pointer
        if (currentSize == maxSize){
            cache.remove(leastRecentlyUsed.key);
            leastRecentlyUsed = leastRecentlyUsed.next;
            leastRecentlyUsed.previous = null;
        }

        // Update cache size, for the first added entry update the LRU pointer
        else if (currentSize < maxSize){
            if (currentSize == 0){
                leastRecentlyUsed = myNode;
            }
            currentSize++;
        }
    }
}