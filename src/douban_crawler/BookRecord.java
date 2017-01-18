package douban_crawler;

public class BookRecord {
	private int serial;
	private int commentsCount;
	private String title;
	private String price;
	private String authorEnu;
	private String authorChn;
	private String copiedYM;
	private String priceInfo;
	private String publisher;
	private float rating;	
	
	public BookRecord() {
		//pass, for now nothing is done in constructors
	}
	
	public int getSerial() {
		return serial;
	}
	public void setSerial(int serial) {
		this.serial = serial;
	}
	public int getCommentsCount() {
		return commentsCount;
	}
	public void setCommentsCount(int commentsCount) {
		this.commentsCount = commentsCount;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getPrice() {
		return price;
	}
	public void setPrice(String price) {
		this.price = price;
	}
	public String getPublisher() {
		return publisher;
	}
	public void setPublisher(String publisher) {
		this.publisher = publisher;
	}
	public float getRating() {
		return rating;
	}
	public void setRating(float rating) {
		this.rating = rating;
	}

	public String getAuthorEnu() {
		return authorEnu;
	}

	public void setAuthorEnu(String authorEnu) {
		this.authorEnu = authorEnu;
	}

	public String getAuthorChn() {
		return authorChn;
	}

	public void setAuthorChn(String authorChn) {
		this.authorChn = authorChn;
	}

	public String getCopiedYM() {
		return copiedYM;
	}

	public void setCopiedYM(String copiedYM) {
		this.copiedYM = copiedYM;
	}

	public String getPriceInfo() {
		return priceInfo;
	}

	public void setPriceInfo(String priceInfo) {
		this.priceInfo = priceInfo;
	}
	
	// todo...
	// will use JUnit on this later
	public void test() {
		System.out.println("serial is " + this.getSerial());
		System.out.println("title is " + this.getTitle());
		System.out.println("rating is " + this.getRating());
		System.out.println("comment count is " + this.getCommentsCount());
		System.out.println("price is " + this.getPrice());
		System.out.println("publisher is " + this.getPublisher());
		System.out.println("Chinese author is " + this.getAuthorChn());
		System.out.println("English author is " + this.getAuthorEnu());
		System.out.println("Price Info is " + this.getPriceInfo());
		System.out.println("Copied year and month is " + this.getCopiedYM());
	}
	
}
