package com.example;

import org.example.MethodGen;
import org.example.VerifierConstraintViolatedException;
import org.example.VerificationResult;

public class Simple {
    // Target method, based on some code in BCEL Util that caused a crash.
    public VerificationResult bar(MethodGen mg) {
        try {
            baz();
        } catch (final VerifierConstraintViolatedException ce) {
            ce.extendMessage("Constraint violated in method '" + mg + "':\n", "");
            return new VerificationResult(VerificationResult.VERIFIED_REJECTED, ce.getMessage());
        }
        return VerificationResult.OK;
    }

    public void baz() throws VerifierConstraintViolatedException {
        // content doesn't matter
    }
}
