package me.ragan262.questernpcs.remoteentities;

import de.kumpelblase2.remoteentities.api.features.RemoteFeature;
import de.kumpelblase2.remoteentities.persistence.SerializeAs;

public class QuesterFeature extends RemoteFeature {
	
	@SerializeAs(pos = 1)
	protected int holder = -1;
	
	public QuesterFeature() {
		super("QUESTER");
	}
	
	public QuesterFeature(int holder) {
		this();
		this.holder = holder;
	}
	
	public int getHolderID() {
		return holder;
	}
	
	public void setHolderID(final int newID) {
		holder = newID;
	}
}
