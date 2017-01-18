package douban_crawler;

import java.util.Comparator;

public class SortByRating implements Comparator{
	public int compare(Object o1, Object o2) {
		BookRecord br1 = (BookRecord)o1;
		BookRecord br2 = (BookRecord)o2;
		if (br1.getRating() > br2.getRating()) {
			return 1;
		}
		return 0;
	}
}
