package com.fray.evo.util;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests the EcYabotEncoder class.
 * @author mike.angstadt
 *
 */
public class EcYabotEncoderTest {
	/**
	 * Tests what happens when no build steps are added to the encoder.
	 */
	@Test
	public void testNoBuildSteps() {
		EcYabotEncoder encoder = new EcYabotEncoder("Build name", "John Smith", "This is my build order.");
		String yabot = encoder.done();
		Assert.assertEquals("1 [i] Build name | 11 | John Smith | This is my build order. [/i]", yabot);
	}
	
	/**
	 * Tests what happens when the default values are used for a build step.
	 */
	@Test
	public void testDefaults(){
		EcYabotEncoder encoder = new EcYabotEncoder("Build name", "John Smith", "This is my build order.");
		encoder.next(); //add a new build step without setting any of its values
		String yabot = encoder.done();
		Assert.assertEquals("1 [i] Build name | 11 | John Smith | This is my build order. [/i] [s] 0 0 0 0:0 1 0 0 0   [/s]", yabot);
	}

	/**
	 * Tests adding multiple build steps.
	 */
	@Test
	public void testBuildSteps() {
		EcYabotEncoder encoder = new EcYabotEncoder("Build name", "John Smith", "This is my build order.");
		encoder.supply(8).minerals(27).gas(0).timestamp("0:22").action(EcYabotEncoder.Action.Extractor).next(); //using action() method, no tag
		encoder.supply(7).minerals(203).gas(0).timestamp("1:06").type(0).item(41).next(); //using type() and item() methods, no tag
		encoder.supply(8).minerals(3).gas(20).timestamp("1:52").type(0).item(0).tag("Add_1_drone_to_gas").next();
		String yabot = encoder.done();
		Assert.assertEquals("1 [i] Build name | 11 | John Smith | This is my build order. [/i] [s] 8 27 0 0:22 1 0 35 0   | 7 203 0 1:06 1 0 41 0   | 8 3 20 1:52 1 0 0 0 Add_1_drone_to_gas [/s]", yabot);
	}
	
	/**
	 * Tests to make sure the done() method resets the encoder.
	 */
	@Test
	public void testReset() {
		EcYabotEncoder encoder = new EcYabotEncoder("Build name", "John Smith", "This is my build order.");
		encoder.supply(8).minerals(27).gas(0).timestamp("0:22").action(EcYabotEncoder.Action.Extractor).next();
		encoder.supply(7).minerals(203).gas(0).timestamp("1:06").type(0).item(41).next();
		encoder.supply(8).minerals(3).gas(20).timestamp("1:52").type(0).item(0).tag("Add_1_drone_to_gas").next();
		encoder.done();

		//make sure the class resets itself after calling done()
		//name, author, and description should remain the same, but the build order steps should be cleared
		encoder.supply(10).minerals(55).gas(10).timestamp("1:11").type(0).item(35).next();
		String yabot = encoder.done();
		Assert.assertEquals("1 [i] Build name | 11 | John Smith | This is my build order. [/i] [s] 10 55 10 1:11 1 0 35 0   [/s]", yabot);
	}
}
