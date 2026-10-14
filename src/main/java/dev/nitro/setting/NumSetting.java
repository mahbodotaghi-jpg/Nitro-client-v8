package dev.nitro.setting;

public class NumSetting extends Setting {
	public double value;
	public final double min, max;
	public final boolean integer;

	public NumSetting(String name, double value, double min, double max, boolean integer) {
		super(name);
		this.min = min;
		this.max = max;
		this.integer = integer;
		set(value);
	}

	public int i() {
		return (int) Math.round(value);
	}

	public void set(double v) {
		v = Math.max(min, Math.min(max, v));
		value = integer ? Math.round(v) : Math.round(v * 100.0) / 100.0;
	}
}
