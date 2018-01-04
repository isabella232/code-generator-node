/*
 *
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2018 David Luna.
 *
 */

package com.luna.authentication;

import com.google.inject.assistedinject.Assisted;
import com.sun.identity.sm.RequiredValueValidator;

import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.*;

import javax.inject.Inject;

import java.security.SecureRandom;

/** 
 * A node that generates codes and stores them in a provided location.
 */
@Node.Metadata(outcomeProvider  = SingleOutcomeNode.OutcomeProvider.class,
               configClass      = CodeGeneratorNode.Config.class)
public class CodeGeneratorNode extends SingleOutcomeNode {

    private final Config config;

    /**
     * Configuration for the node.
     */
    public interface Config {
        @Attribute(order = 100, validators = {RequiredValueValidator.class})
        default String sharedStateKey() { return "generatedCode"; }

        @Attribute(order = 200, validators = {RequiredValueValidator.class})
        default CodeGeneratorType codeGeneratorType() { return CodeGeneratorType.UUID; }

        @Attribute(order = 300, validators = {RequiredValueValidator.class})
        default int generatedCodeLength() { return 10; }

        @Attribute(order = 400)
        default String generatedCodeAlphabet() { return "01AaBbCc"; }
    }

    /**
     * Create the node.
     *
     * @param config The service config.
     * @throws NodeProcessException If the configuration was not valid.
     */
    @Inject
    public CodeGeneratorNode(@Assisted Config config) throws NodeProcessException {
        this.config = config;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        String storageLocation = config.sharedStateKey();
        String generatedCode = generateCode(config.codeGeneratorType(), config.generatedCodeLength());

        return goToNext().replaceSharedState(context.sharedState.copy().put(storageLocation, generatedCode)).build();
    }

    private String generateCode(CodeGeneratorType codeGeneratorType, int length) {
        return codeGeneratorType.generate(length, config.generatedCodeAlphabet());
    }

    /**
     * Enum representing various alphabets to be used for code generation.
     */
    public enum CodeGeneratorType implements CodeGeneration {

        CodeAlphabet(CodeGeneratorType::internalGenerate),

        /** Will generate a full UUID. **/
        UUID((i, s) -> { return java.util.UUID.randomUUID().toString(); }),

        Alphanumeric((i, s) -> {
            return internalGenerate(i,"0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"); }),

        DeviceCode((i, s) -> {
            return internalGenerate(i, "234567ACDEFGHJKLMNPQRSTWXYZabcdefhijkmnopqrstwxyz"); }),

        Base58((i, s) -> {
            return internalGenerate(i, "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"); }),

        Base64((i, s) -> {
            return internalGenerate(i, "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz+/"); }),

        Base64Url((i, s) -> {
            return internalGenerate(i, "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz-_"); }),

        Alphabet((i, s) -> {
            return internalGenerate(i, "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"); }),

        AlphabetUppercase((i, s) -> {
            return internalGenerate(i, "ABCDEFGHIJKLMNOPQRSTUVWXYZ"); }),

        AlphabetLowercase((i, s) -> {
            return internalGenerate(i, "abcdefghijklmnopqrstuvwxyz"); }),

        Decimal((i, s) -> {
            return internalGenerate(i, "0123456789"); }),

        Hex((i, s) -> {
            return internalGenerate(i, "0123456789abcdef"); }),

        Binary((i, s) -> {
            return internalGenerate(i, "01"); });

        private final CodeGeneration source;
        private final static SecureRandom secureRandom = new SecureRandom();

        CodeGeneratorType(final CodeGeneration source) {
            this.source = source;
        }

        private static String internalGenerate(int length, String chars) {
            StringBuilder codeBuilder = new StringBuilder(length);

            for (int k = 0; k < length; k++) {
                codeBuilder.append(chars.charAt(secureRandom.nextInt(chars.length())));
            }

            return codeBuilder.toString();}

        @Override
        public String generate(int length, String userInput) {
            return source.generate(length, userInput);
        }

    }
}