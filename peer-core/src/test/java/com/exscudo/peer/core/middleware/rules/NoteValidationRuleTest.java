package com.exscudo.peer.core.middleware.rules;

import java.util.Random;

import com.exscudo.peer.core.Builder;
import com.exscudo.peer.core.Constant;
import com.exscudo.peer.core.Signer;
import com.exscudo.peer.core.common.exceptions.ValidateException;
import com.exscudo.peer.core.crypto.ISigner;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.middleware.AbstractValidationRuleTest;
import com.exscudo.peer.core.middleware.IValidationRule;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class NoteValidationRuleTest extends AbstractValidationRuleTest {
    private final static String alphabet = "0123456789 abcdefghijklmnoprstuvwxyz ABCDEFGHIJKLMNOPQRSTUVWXYZ #@*-_";
    private NoteValidationRule rule;
    private ISigner sender = new Signer("112233445566778899aabbccddeeff00112233445566778899aabbccddeeff00");

    @Override
    protected IValidationRule getValidationRule() {
        return rule;
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        rule = new NoteValidationRule(fork, timeProvider);
    }

    @Test
    public void forbidden_note_success() throws Exception {
        Mockito.when(fork.getMaxNoteLength(ArgumentMatchers.anyInt())).thenReturn(0);

        Transaction tx = Builder.newTransaction(timeProvider).build(networkID, sender);
        validate(tx);
    }

    @Test
    public void forbidden_note_invalid() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Invalid note");

        Mockito.when(fork.getMaxNoteLength(ArgumentMatchers.anyInt())).thenReturn(0);
        Transaction tx = Builder.newTransaction(timeProvider).note("Note").build(networkID, sender);
        validate(tx);
    }

    @Test
    public void success() throws Exception {
        Transaction tx = Builder.newTransaction(timeProvider).note("Note").build(networkID, sender);
        validate(tx);
    }

    @Test
    public void success_without_note() throws Exception {
        Transaction tx = Builder.newTransaction(timeProvider).build(networkID, sender);
        validate(tx);
    }

    @Test
    public void empty_note() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Invalid note");

        Transaction tx = Builder.newTransaction(timeProvider).note("").build(networkID, sender);
        validate(tx);
    }

    @Test
    public void invalid_note_length() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Invalid note");

        Random random = new Random();
        char[] symbols = alphabet.toCharArray();
        char[] buffer = new char[Constant.TRANSACTION_NOTE_MAX_LENGTH + 1];
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = symbols[random.nextInt(symbols.length)];
        }

        Transaction tx = Builder.newTransaction(timeProvider).note(new String(buffer)).build(networkID, sender);
        validate(tx);
    }

    @Test
    public void illegal_symbol() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Invalid note");

        Transaction tx = Builder.newTransaction(timeProvider).note("%note%").build(networkID, sender);
        validate(tx);
    }

    @Test
    public void check_alphabet() throws Exception {
        Transaction tx = Builder.newTransaction(timeProvider).note(alphabet).build(networkID, sender);
        validate(tx);
    }
}
