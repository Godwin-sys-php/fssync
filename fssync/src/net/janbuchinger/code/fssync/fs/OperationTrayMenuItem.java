package net.janbuchinger.code.fssync.fs;

import java.awt.MenuItem;

@SuppressWarnings("serial")
public class OperationTrayMenuItem extends MenuItem {
	private final Operation operation;
	
	public OperationTrayMenuItem(Operation operation) {
		super(operation.toString());
		this.operation = operation;
	}

	public final Operation getOperation() {
		return operation;
	}
}
