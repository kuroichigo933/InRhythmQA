package helpers;

import org.junit.Assert;

public class PaginationHelper
{
    String[] stringListSet;
    double itemsPerPageSet;

    public PaginationHelper(String[] stringList, int itemsPerPage)
    {
        stringListSet = stringList;
        itemsPerPageSet = itemsPerPage;
    }

    public int pageCount()
    {
        validateValidStringList(stringListSet);
        return (int) Math.ceil(stringListSet.length / itemsPerPageSet); // Ceil rounds up regardless of number, 1.2 -> 2
    }

    public int itemCount()
    {
        validateValidStringList(stringListSet);
        return stringListSet.length;
    }

    // utilize method to get count of items on last page
    private int itemCountOnLastPage()
    {
        // first we check if remainder exists, by determining if there are some half-full pages
        Double remainder = pageCount() - (itemCount() / itemsPerPageSet);
        if (remainder == 0) // if no remainder exists, last page is full and thus is max page count
        {
            return (int) itemsPerPageSet;
        }
        else
        {
            // if remainder exists, we can simply multiply it by items allowed per page to determine how many on last
            // page
            return (int) (remainder * itemsPerPageSet);
        }
    }

    public int pageItemCount(int pageNumber)
    {
        // subtracting by 1 here due to zero-based index
        if (pageNumber >= 0 && pageNumber < pageCount() - 1) // if specified page is not last, then it is max # allowed
            // per page
        {
            return (int) itemsPerPageSet;
        }
        else if (pageNumber == pageCount() - 1) // if we are last page, then we calculate how may items on last page
        {
            return itemCountOnLastPage();
        }
        System.out.println("Page number must be less than or equal to " + (pageCount() - 1) + "(zero indexing)");
        return -1; // only occurs if page number is greater than possible pages
    }

    public int pageIndex(int index)
    {
        if (index <= 0 || index > itemCount()) // if index is negative or greater than item count, we throw -1 error
        {
            System.out.println("Page index must be greater than 0 and less than total item count");
            return -1;
        }

        return (int) Math.ceil(index / itemsPerPageSet) - 1; // otherwise, we simply divide item index by maxPage set
        // and always round up. We minus 1 due to 0-based indexing
    }

    public void validateValidStringList(String[] stringList)
    {
        if (stringList == null || stringList.length == 0)
        {
            Assert.fail("List is empty. Please input a valid list.");
        }
    }
}