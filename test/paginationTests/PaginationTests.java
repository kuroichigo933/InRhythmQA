package paginationTests;

import helpers.PaginationHelper;
import org.junit.Assert;
import org.junit.Test;

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
    }

    @Test
    public void validatePageItemCount_OutOfBounds()
    {
        Assert.assertEquals(-1, helper.pageItemCount(2));
    }

    @Test
    public void validateNegativePageNumber()
    {
        Assert.assertEquals(-1, helper.pageItemCount(-1));
    }

    @Test
    public void validatePageIndex()
    {
        Assert.assertEquals(1, helper.pageIndex(5));
        Assert.assertEquals(0, helper.pageIndex(2));
        Assert.assertEquals(-1, helper.pageIndex(20));
    }

    @Test
    public void validatePageIndexOfZero()
    {
        Assert.assertEquals(-1, helper.pageIndex(0));
    }

    @Test
    public void validateNegativePageIndex()
    {
        Assert.assertEquals(-1, helper.pageIndex(-10));
    }

    @Test
    public void stringListIsProperlyValidatedOnPageCount()
    {
        helper = new PaginationHelper(null, 2);
        try
        {
            helper.pageCount();
            throw new AssertionError("Error was not thrown.");
        }
        catch (AssertionError e)
        {
            Assert.assertEquals("List is empty. Please input a valid list.", e.getMessage());
        }

    }

    @Test
    public void stringListIsProperlyValidatedOnItemCount()
    {
        helper = new PaginationHelper(null, 2);
        try
        {
            helper.itemCount();
            throw new AssertionError("Error was not thrown.");
        }
        catch (AssertionError e)
        {
            Assert.assertEquals("List is empty. Please input a valid list.", e.getMessage());
        }

    }


}