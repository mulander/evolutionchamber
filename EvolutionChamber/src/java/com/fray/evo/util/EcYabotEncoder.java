package com.fray.evo.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Creates YABOT build order strings.
 * 
 * @author mike.angstadt
 * 
 */
public class EcYabotEncoder {
	/**
	 * The name of the build order.
	 */
	private String name;

	/**
	 * The name of the person who generated this build order.
	 */
	private String author;

	/**
	 * A description of the build order.
	 */
	private String description;

	/**
	 * The current YABOT build step.
	 */
	private BuildStep curStep = new BuildStep();

	/**
	 * The list of completed YABOT build steps.
	 */
	private List<BuildStep> steps = new ArrayList<BuildStep>();

	/**
	 * Constructor.
	 * 
	 * @param name the name of the build order
	 * @param author the author of the build order
	 * @param description a description for the build order
	 */
	public EcYabotEncoder(String name, String author, String description) {
		this.name = name;
		this.author = author;
		this.description = description;
	}

	/**
	 * Sets the supply of the current build step.
	 * 
	 * @param supply
	 * @return this
	 */
	public EcYabotEncoder supply(int supply) {
		curStep.supply = supply;
		return this;
	}

	/**
	 * Sets the minerals of the current build step.
	 * 
	 * @param minerals
	 * @return this
	 */
	public EcYabotEncoder minerals(int minerals) {
		curStep.minerals = minerals;
		return this;
	}

	/**
	 * Sets the gas of the current build step.
	 * 
	 * @param gas
	 * @return this
	 */
	public EcYabotEncoder gas(int gas) {
		curStep.gas = gas;
		return this;
	}

	/**
	 * Sets the timestamp of the current build step.
	 * 
	 * @param timestamp should be in the format "h:m:s". For example, "1:04:32"
	 * for one hour, four minutes, thirty-two seconds.
	 * @return
	 */
	public EcYabotEncoder timestamp(String timestamp) {
		curStep.timestamp = timestamp;
		return this;
	}

	/**
	 * Sets the type number of the current build step.
	 * 
	 * @param type
	 * @return this
	 */
	public EcYabotEncoder type(int type) {
		curStep.type = type;
		return this;
	}

	/**
	 * Sets the item number of the current build step.
	 * 
	 * @param item
	 * @return this
	 */
	public EcYabotEncoder item(int item) {
		curStep.item = item;
		return this;
	}

	/**
	 * Sets whether the current build step is a cancel operation or not.
	 * Defaults to false.
	 * 
	 * @param cancel
	 * @return this
	 */
	public EcYabotEncoder cancel(boolean cancel) {
		curStep.cancel = cancel;
		return this;
	}

	/**
	 * Sets the tag of the current build step. Defaults to empty string.
	 * 
	 * @param tag
	 * @return this
	 */
	public EcYabotEncoder tag(String tag) {
		curStep.tag = tag;
		return this;
	}

	/**
	 * Completes the current build step and advances to a new build step.
	 * 
	 * @return this
	 */
	public EcYabotEncoder next() {
		steps.add(curStep);
		curStep = new BuildStep();
		return this;
	}

	/**
	 * Generates the entire YABOT string. Also resets the encoder so that the
	 * same object can be used to generate a new YABOT string.
	 * 
	 * @return the YABOT string
	 */
	public String done() {
		StringBuilder sb = new StringBuilder();

		sb.append("1 [i] " + name + " | 11 | " + author + " | " + description + " [/i]");
		if (steps.size() > 0) {
			sb.append(" [s] ");

			sb.append(steps.get(0).toString());
			for (int i = 1; i < steps.size(); i++) {
				BuildStep entry = steps.get(i);
				sb.append(" | " + entry.toString());
			}
			sb.append(" [/s]");
		}

		//reset so a new YABOT string can be created with the same object
		curStep = new BuildStep();
		steps.clear();

		return sb.toString();
	}

	/**
	 * Represents a build order step in a YABOT string.
	 * 
	 * @author mike.angstadt
	 * 
	 */
	private static class BuildStep {
		public int supply = 0;
		public int minerals = 0;
		public int gas = 0;
		public String timestamp = "0:0";
		public int type = 0;
		public int item = 0;
		public boolean cancel = false;
		public String tag = " ";

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(supply);
			sb.append(" " + minerals);
			sb.append(" " + gas);
			sb.append(" " + timestamp);
			sb.append(" 1");
			sb.append(" " + type);
			sb.append(" " + item);
			sb.append(" " + (cancel ? "1" : "0"));
			sb.append(" " + tag);
			return sb.toString();
		}
	}
}
