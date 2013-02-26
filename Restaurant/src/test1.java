import static org.junit.Assert.*;

import org.junit.Test;


public class test1 {

	@Test
	public void test() {
		boolean foo = 1+1==2;
		assertTrue(true);
		assertTrue(foo);
		
		assertTrue("This will not fail", true);
		//assertTrue("This will fail", false);
	}

}
