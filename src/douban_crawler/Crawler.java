
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.lang.Thread;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import jxl.Workbook;
import jxl.write.Label;
import jxl.write.WritableCellFormat;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Crawler {
	private final static String commentsCountQuery = "span.pl";
	private final static String titleQuery = "h2 > a";
	private final static String priceInfoQuery = "span.buy-info > a";
	private final static String lineInfoQuery = "div.pub";
	private final static String ratingQuery = "span.rating_nums";
	ArrayList<String> allUrls = new ArrayList<String>();
	ArrayList<BookRecord> brs = new ArrayList<BookRecord>();
	ArrayList<String> urlsToBeCrawled = new ArrayList<String>();
	HashMap<String, Integer> depthMap = new HashMap<String, Integer>();
	int page = 0;
	int booksCount = 0;
	int threadCount = 5;
	int waitThreadCount = 0;
	int count = 0;
	int maxDepth = 2;
	int leastRatingIndex = -1;
	float leastRating = Float.MAX_VALUE;
	String url = "https://book.douban.com/tag/%E7%BC%96%E7%A8%8B?type=S";
	public static final Object signal = new Object();
	
	private static void fillUrls(String url, ArrayList<String> urls, HashMap<String, Integer> depth) {
		try {
			Document doc = Jsoup
				.connect(url)
				.userAgent("Mozilla")
				.cookie("gr_user_id", "081a1753-9fd5-4ee5-b705-a207fdbd9a46")
				.timeout(5000)
				.get();
			Element maxPageEle = doc.select("div.paginator > a").last();
			int maxPage = Integer.parseInt(maxPageEle.text());
			System.out.println(maxPage);
			for(int i = 0; i <= maxPage; i++) {
				urls.add(url + "&start=" + i * 20);
				depth.put(url + "&start=" + i * 20, 1);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	@SuppressWarnings("unchecked")
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		try {
			Crawler crawler = new Crawler();
			System.setProperty("https.proxyHost", "192.168.2.101");
			System.setProperty("https.proxyPort", "1080");

			fillUrls(crawler.url, crawler.urlsToBeCrawled, crawler.depthMap);
			crawler.addUrl(crawler.url, 1);
			long start = System.currentTimeMillis();
			System.out.println("Crawling started");
			crawler.begin();
			
			while (true) {
				if (crawler.urlsToBeCrawled.isEmpty() && Thread.activeCount() == 1 || crawler.count == crawler.threadCount) {
					Collections.sort(crawler.brs, new SortByRating());
					writeExcel("C://test1.xls", crawler.brs);

					long end = System.currentTimeMillis();
					System.out.println("Total crawl time" + (end - start) / 1000 + " seconds");
					System.exit(1);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void begin() {
		for (int i = 0; i < threadCount; i++) {
			new Thread(new Runnable(){
				public void run() {
					while (true) {   
						System.out.println("Entering " + Thread.currentThread().getName());  
                        String tmp = getOneUrl();  
                        if(tmp!=null){  
                            crawl(tmp);  
                        }else{  
                            synchronized(signal) {
                                try {  
                                    count++;  
                                    signal.wait();  
                                } catch (InterruptedException e) {  
                                    // TODO Auto-generated catch block  
                                    e.printStackTrace();  
                                }  
                            }  
                              
                              
                        }  
                    }  
				}
			},"thread-" + i).start();
		}
	}
	
	private synchronized String getOneUrl() {
		if (urlsToBeCrawled.isEmpty()) {
			return null;		
		}
		String url = urlsToBeCrawled.get(0);
		urlsToBeCrawled.remove(0);
		
		return url;
	}
	
	private synchronized void addUrl(String url, int depth) {
		urlsToBeCrawled.add(url);
		allUrls.add(url);
		depthMap.put(url, depth);
	}
	
	private void crawl(String url) {
		try {
//			String currentUrl = url + "&start=" + page * 20;
//			page++;
			int depth = depthMap.get(url);
			if (depth > maxDepth) {
				return;
			}
			System.out.println("Current crawling page: " + url + " with depth " + depth);
			Document doc = Jsoup
				.connect(url)
				.userAgent("Mozilla")
				.cookie("gr_user_id", "081a1753-9fd5-4ee5-b705-a207fdbd9a46")
				.timeout(5000)
				.get();
			for (Element e : doc.getElementsByClass("subject-item")) {
				String commentsText = e.select(commentsCountQuery).text();
				int commentsCount = getCommentsCount(commentsText);
				if (commentsCount < 1000) {
					continue;
				}
				booksCount++;
				BookRecord br = new BookRecord();
				br.setCommentsCount(commentsCount);
				br.setPriceInfo(e.select(priceInfoQuery).text());
				String lineInfo = (e.select(lineInfoQuery).text());
				parseLineInfo(br, lineInfo);
				br.setRating(Float.parseFloat(e.select(ratingQuery).text()));
				br.setSerial(booksCount);
				br.setTitle(e.select(titleQuery).attr("title"));
				if (brs.size() < 40) {
					brs.add(br);
					if (br.getRating() < leastRating) {
						leastRating = br.getRating();
						leastRatingIndex = 0;
					} else {
						leastRatingIndex += 1;
					}
				} else {
					if (leastRating >= br.getRating()) {
						continue;
					} else {
						brs.remove(leastRatingIndex);
						brs.add(br);
						leastRating = Float.MAX_VALUE;
						leastRatingIndex = -1;
						for (int i = 0; i < brs.size(); i++) {
							if (brs.get(i).getRating() < leastRating) {
								leastRating = brs.get(i).getRating();
								leastRatingIndex = i;
							}
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static int getCommentsCount(String commentsText) {
		String pattern = "\\d+";
		Pattern r = Pattern.compile(pattern);
		Matcher m = r.matcher(commentsText);
		
		if(m.find() && Integer.parseInt(m.group()) >= 1000) {
			return Integer.parseInt(m.group());
		}
		return 0;
	}
	
	private static void parseLineInfo(BookRecord br, String lineInfo) {
		String[] chunks = lineInfo.split("/");
		
		// If original author is Chinese, then there's no english author, minus one field
		if(chunks.length == 4) {
			br.setAuthorChn(chunks[0]);
			br.setPublisher(chunks[1]);
			br.setCopiedYM(chunks[2]);
			br.setPrice(chunks[3]);
		} else if(chunks.length == 5) {
			br.setAuthorEnu(chunks[0]);
			br.setAuthorChn(chunks[1]);
			br.setPublisher(chunks[2]);
			br.setCopiedYM(chunks[3]);
			br.setPrice(chunks[4]);
		}
	}
	
	private static void writeExcel(String fileName, List<BookRecord> brs) throws IOException, WriteException{
		OutputStream os = new FileOutputStream(new File(fileName));
		WritableWorkbook workbook = Workbook.createWorkbook(os);
		WritableSheet sheet = workbook.createSheet("Douban Top Programming Books", 0);
		
		WritableCellFormat format = new WritableCellFormat();
		format.setAlignment(jxl.format.Alignment.CENTRE);
		format.setVerticalAlignment(jxl.format.VerticalAlignment.CENTRE);
		
		Label rank = new Label(0, 0, "序号");
		Label title = new Label(1, 0, "书名");
		Label rating = new Label(2, 0, "评分");
		Label author = new Label(3, 0, "作者");
		Label publisher = new Label(4, 0, "出版社");
		Label copiedYM = new Label(5, 0, "出版年月");
		Label price = new Label(6, 0, "价格");
		sheet.addCell(rank);
		sheet.addCell(title);
		sheet.addCell(rating);
		sheet.addCell(author);
		sheet.addCell(publisher);
		sheet.addCell(copiedYM);
		sheet.addCell(price);
		
		int rowNum = 1;
		for(int i = brs.size() - 1; i >= 0; i--) {
			Label brSerial = new Label(0, rowNum, rowNum + "");
			Label brTitle = new Label(1, rowNum, brs.get(i).getTitle());
			Label brRating = new Label(2, rowNum, brs.get(i).getRating() + "");
			Label brAuthor = new Label(3, rowNum, brs.get(i).getAuthorEnu() + brs.get(i).getAuthorChn());
			Label brPublisher = new Label(4, rowNum, brs.get(i).getPublisher());
			Label brCopiedYM = new Label(5, rowNum, brs.get(i).getCopiedYM());
			Label brPrice = new Label(6, rowNum, brs.get(i).getPrice());
			sheet.addCell(brSerial);
			sheet.addCell(brTitle);
			sheet.addCell(brRating);
			sheet.addCell(brAuthor);
			sheet.addCell(brPublisher);
			sheet.addCell(brCopiedYM);
			sheet.addCell(brPrice);
			rowNum++;
		}
		workbook.write();
		workbook.close();
		os.close();
	}

}
