package com.apotheotictrims;

public record SettingSpec(String key, String label, double defaultValue, double min, double max,
                          double step, double coarseStep, boolean integral) {
    public double validate(double value) {
        if (!Double.isFinite(value) || value < min || value > max || (integral && value != Math.rint(value))) {
            throw new IllegalArgumentException(label + " must be " + rangeDescription());
        }
        return integral ? Math.rint(value) : value;
    }

    public double clamp(double value) {
        double clamped = Math.max(min, Math.min(max, value));
        return integral ? Math.rint(clamped) : clamped;
    }

    public String rangeDescription() {
        return integral ? (int) min + "-" + (int) max + " (whole number)" : min + "-" + max;
    }

    public String format(double value) {
        if (integral || value == Math.rint(value)) return Long.toString(Math.round(value));
        return String.format(java.util.Locale.ROOT, "%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }
}
