# Simple Java Web Crawler (Multi-Threaded)
***

## Overview
This is a simple multithreaded web crawler written in Java 17. 
Given a seed URL and maximum number of pages, it will begin crawling from the seed URL, 
printing each page's title and meta description to the console.
  
Please note that this version of the web-crawler is something of a work in progress. The latest thing I've added is
the handling for robots.txt, which currently also has no test cases.

## Instructions
To run the web crawler, first start by cloning the repository and installing Maven if you don't already have it.

You can execute the program with default arguments using the following command for Maven:
```
mvn compile exec:java
```
This will set the crawl parameters as found in the pom.xml:
```
<arguments>
    <argument>https://crawler-test.com/</argument>
    <argument>500</argument>
</arguments>
```
The crawler will crawl using the seed URL https://crawler-test.com/ to a maximum of 500 pages crawled.

To specify your own parameters, you can override the default arguments by using the following command:
```
mvn compile exec:java -Dexec.arguments=SeedURL,MaxPagesToCrawl
```
Where SeedURL should be replaced with the seed URL, and MaxPagesToCrawl should be replaced with the crawl limit.

For example:
```
mvn compile exec:java -Dexec.arguments=https://crawler-test.com/,500
```

For this multithreaded version of the crawler, you can also select the amount of threads to use by changing the code
here in SpiderManager:
```
private static final Integer MAX_THREADS = 10;
```

## Things To Improve

### Redirect Handling
Currently, if a link redirects to a page we have already crawled,
then we have a scenario where we crawl the same page twice.

### Domain Limiting
We do not currently impose any restrictions on pages to crawl when it comes to the domain, this means that we will crawl
external links just the same as we do internal links.

This can cause us to go down a rabbit hole in terms of crawling a website we're not interested in.

## Development Notes

### Multi-Threading
I have used the ExecutorService to handle multi-threading in this application. 
  
The way it works is that the SpiderManager submits an initial crawl task to the service, with the work to be done by a SpiderWorker.
The SpiderManager then poll()'s the queue to get back new links from the seed page that the SpiderWorker has crawled.
Then it submits new tasks for other SpiderWorkers to the end of the queue. These tasks are picked up as threads become
available, up to the maximum amount of threads.  
  
Potential concurrency issues are handled by only allowing the main thread (i.e the SpiderManager) to either submit new tasks
or read/write from the HashSet which tracks crawled/queued pages.    

When I created version 1 of this application, I was passing a reference to the SpiderManager object to the workers, and
the workers were manipulating the set of crawled/queued, and also the queue itself, so I had all kinds of problems
with concurrency. I also found it very difficult to test, which is a sign of bad design. What you see here is version
2 of the application which is:
- Easier to unit test.  
- Free from concurrency issues.
- Better organised.
  
### JavaDocs & Comments
I have not included any JavaDocs in this program, due to the fact that the program is small, and I have endeavored
to name and structure methods in such a way that they are extremely self-explanatory.  
  
Similarly, you will find minimal comments in the code, and should struggle to find a place to put a comment which would
not be redundant.  
  
If whilst reading the code, you think to yourself 
"this could have done with a comment/JavaDoc because it isn't entirely clear what's going on", 
then I would argue the shortfall is due to my code structure, and not the lack of comment/documentation.  
  
In a professional setting, JavaDocs/comments should be written as per company guidelines.