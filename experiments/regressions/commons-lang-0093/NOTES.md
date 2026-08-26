# Diagnosis

`ToStringBuilder#append(int[])`, the target, has one statement:

```java
public ToStringBuilder append(final int[] array) {
    style.append(buffer, null, array, null);   // ToStringBuilder.java:471
    return this;
}
```

`ToStringStyle` declares nine overloads of `append(StringBuffer, String, X[], Boolean)`, one per
primitive array type plus `Object[]`. The slice keeps exactly one of them. The base build keeps
the `int[]` overload; this branch keeps the `long[]` one, so the preserved call no longer
type-checks:

```
ToStringBuilder.java:10: error: incompatible types: int[] cannot be converted to long[]
    style.append(buffer, null, array, null);
```

There is no widening conversion between array types (JLS 5.1.5 permits an array type to widen
only to `Object`, `Cloneable` or `Serializable`, and JLS 10.1 makes `int[]` and `long[]` unrelated
types), so `long[]` was never an applicable candidate for this call. The branch is choosing among
the overloads by something other than applicability.

It is deterministic: three consecutive runs of the branch build on this target all emitted
`long[]`, so this is not a hash-iteration-order effect.

`ToStringStyle` is a real class in the input, not a synthesized one, and both builds emit the same
four files -- the only difference between the two outputs is this one parameter type.

Note that `long[]` is neither the first nor the last of the nine overloads in source order
(`boolean[] byte[] char[] double[] float[] int[] long[] Object[] short[]`); it is the one directly
after the correct `int[]`.
