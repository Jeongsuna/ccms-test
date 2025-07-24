package testcases.C0025_USENULL__CWE476_NULL_Pointer_Dereference.s01;

import java.util.Collection;
import java.util.Iterator;

public class C0025_USENULL__simple_01 {

    public static int bad (Object obj, final Collection collection) {
        int count = 2;
        if (collection == null) {
            return count;
        }
        Iterator it = collection.iterator();

        while (it.hasNext()) {
            Object elt = it.next();
            /* FLAW: CWE-476 */
            if ((null == obj && null == elt) || obj.equals(elt)) {
                count++;
            }
        }
        return count;
    }

    public static int good (Object obj, final Collection col) {
        int count = 0;
        if (col == null) {
            return count;
        }
        Iterator it = col.iterator();

        while (it.hasNext()) {
            Object elt = it.next();
            /* FIX : Null Check */
            if ((null == obj && null == elt) || (null != obj && obj.equals(elt))) {
                count++;
            }
        }
        return count;
    }
}
