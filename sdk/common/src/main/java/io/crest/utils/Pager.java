package io.crest.utils;

/**
 * Generic pagination wrapper carrying records and total counts.
 */
public class Pager<T> {
    /**
     * Page records or any list-like payload returned to callers.
     */
    private T listObject;
    /**
     * Total number of records across all pages.
     */
    private long itemCount;
    /**
     * Total number of pages.
     */
    private long pageCount;

    /**
     * Creates an empty pager for frameworks that require a no-args constructor.
     */
    public Pager() {
    }

    /**
     * Creates a pager with records, item count, and page count.
     */
    public Pager(T listObject, long itemCount, long pageCount) {
        this.listObject = listObject;
        this.itemCount = itemCount;
        this.pageCount = pageCount;
    }

    /**
     * Returns the total number of pages.
     */
    public long getPageCount() {
        return pageCount;
    }

    /**
     * Sets the total number of pages.
     */
    public void setPageCount(long pageCount) {
        this.pageCount = pageCount;
    }


    /**
     * Returns the total number of records.
     */
    public long getItemCount() {
        return itemCount;
    }

    /**
     * Sets the total number of records.
     */
    public void setItemCount(long itemCount) {
        this.itemCount = itemCount;
    }

    /**
     * Returns the page records payload.
     */
    public T getListObject() {
        return listObject;
    }

    /**
     * Sets the page records payload.
     */
    public void setListObject(T listObject) {
        this.listObject = listObject;
    }
}
