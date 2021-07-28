package paginationTests;

import org.junit.Assert;
import org.junit.Test;

import helpers.PaginationHelper;

public class PaginationTests
{
    PaginationHelper helper = new PaginationHelper(new String[] {"a", "b", "c", "d", "e", "f"}, 4);

    @Test
    public void validatePageCount()
    {
        Assert.assertEquals(2, helper.pageCount());
    }

    @Test
    public void validateItemCount()
    {
        Assert.assertEquals(6, helper.itemCount());
    }

    @Test
    public void validatePageItemCount()
    {
        Assert.assertEquals(4, helper.pageItemCount(0));
        Assert.assertEquals(2, helper.pageItemCount(1));
        Assert.assertEquals(-1, helper.pageItemCount(2));
    }

    @Test
    public void validatePageIndex()
    {
        Assert.assertEquals( 1, helper.pageIndex(5));
        Assert.assertEquals(0, helper.pageIndex(2));
        Assert.assertEquals(-1, helper.pageIndex(20));
        Assert.assertEquals(-1, helper.pageIndex(-10));
    }
}