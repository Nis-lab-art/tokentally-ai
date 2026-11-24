package tokentally.util;

import java.util.ArrayList;
import java.util.Comparator;

// binary heap in an array list
// the item that comes first by the comparator is always on top
// pass a comparator for a min heap and the reverse for a max heap
public class Heap<T> {

    private final ArrayList<T> items = new ArrayList<>();
    private final Comparator<T> order;

    public Heap(Comparator<T> order) {
        this.order = order;
    }

    public int size() {
        return items.size();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public T peek() {
        return items.isEmpty() ? null : items.get(0);
    }

    // add at the end and move it up while it beats its parent
    public void push(T item) {
        items.add(item);
        int i = items.size() - 1;
        while (i > 0) {
            int parent = (i - 1) / 2;
            if (order.compare(items.get(i), items.get(parent)) >= 0) {
                break;
            }
            swap(i, parent);
            i = parent;
        }
    }

    // take the top, put the last item on top and move it down
    public T pop() {
        if (items.isEmpty()) {
            return null;
        }
        T top = items.get(0);
        T last = items.remove(items.size() - 1);
        if (!items.isEmpty()) {
            items.set(0, last);
            int i = 0;
            while (true) {
                int left = 2 * i + 1;
                int right = left + 1;
                int best = i;
                if (left < items.size() && order.compare(items.get(left), items.get(best)) < 0) {
                    best = left;
                }
                if (right < items.size() && order.compare(items.get(right), items.get(best)) < 0) {
                    best = right;
                }
                if (best == i) {
                    break;
                }
                swap(i, best);
                i = best;
            }
        }
        return top;
    }

    private void swap(int a, int b) {
        T t = items.get(a);
        items.set(a, items.get(b));
        items.set(b, t);
    }
}
