import com.sun.istack.internal.NotNull;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ImmutableQueueImpl<T> implements ImmutableQueue<T> {

    static private int globalVersion = 0;
    static private LinkedList<LinkedList<Integer>> skipList;

    private Data head;
    private Data tail;

    // O(logN)
    public ImmutableQueueImpl<T> enQueue(T t) {
        ImmutableQueueImpl<T> ret = shallowCopy();

        int prevVersion = 0;
        if(ret.tail != null) prevVersion = ret.tail.version;

        if(ret.head == null) {
            ret.head = ret.tail = new Data(t, globalVersion);
        } else {
            Data data = new Data(t, globalVersion);
            ret.tail = ret.tail.addNextData(data);
        }
        globalVersion++;

        ret.tail.setSkipList(prevVersion);

        return ret;
    }

    // O(logN)
    public ImmutableQueueImpl<T> deQueue() {
        ImmutableQueueImpl<T> ret = shallowCopy();
        if(ret.head == ret.tail) {
            ret.head = ret.tail = null;
            return ret;
        }

        ret.head = ret.next();

        return ret;
    }

    public T head() {
        //Serialization of object
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bos);
            out.writeObject(head.data);

            //De-serialization of object
            ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
            ObjectInputStream in = new ObjectInputStream(bis);
            return (T) in.readObject();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return head.data;
    }

    public boolean isEmpty() {
        return head == null;
    }

    private Data next() {
        if(head == tail) {
            return null;
        }

        int nextHeadVersion = tail.version;
        int skipListIndex = skipList.get(tail.version).size()-1;
        while(head.version < nextHeadVersion && skipListIndex >= 0) {
            if(skipListIndex < skipList.get(nextHeadVersion).size() &&
               skipList.get(nextHeadVersion).get(skipListIndex) > head.version) {
                nextHeadVersion = skipList.get(nextHeadVersion).get(skipListIndex);
            } else {
                skipListIndex--;
            }
        }

        int l = 0, r = head.nextList.size();
        while(r-l>1) {
            int mid = (l+r)/2;
            if(head.nextList.get(mid).version <= nextHeadVersion) {
                l = mid;
            } else {
                r = mid;
            }
        }

        return head.nextList.get(l);
    }

    public String toString() {
        StringBuilder ret = new StringBuilder();
        ImmutableQueueImpl<T> p = shallowCopy();
        while(p.head != null) {
            ret.append(p.head.data.toString());
            p.head = p.next();
        }
        return ret.toString();
    }

    private ImmutableQueueImpl<T> shallowCopy() {
        ImmutableQueueImpl<T> ret = new ImmutableQueueImpl<T>();
        ret.head = this.head;
        ret.tail = this.tail;
        return ret;
    }

    private class Data {
        private T data;
        private int version;
        private List<Data> nextList;

        private Data(T data, int version) {
            this.data = data;
            this.version = version;
            nextList = new ArrayList<Data>();
        }

        private void setSkipList(int prevVersion) {
            if(skipList == null) {
                skipList = new LinkedList<LinkedList<Integer>>();
            }

            skipList.add(new LinkedList<Integer>());
            for(int level = 0; true; level++) {
                skipList.getLast().addLast(prevVersion);
                if (prevVersion == 0) break;
                if (skipList.get(prevVersion).size() <= level) {
                    prevVersion = 0;
                } else {
                    prevVersion = skipList.get(prevVersion).get(level);
                }
            }
        }

        private Data addNextData(Data nextData) {
            nextList.add(nextData);
            return nextData;
        }
    }
}
