package com.exscudo.peer.eon.tx.rules;

import java.util.Random;

import com.exscudo.peer.MockSigner;
import com.exscudo.peer.core.common.exceptions.ValidateException;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.transaction.IValidationRule;
import com.exscudo.peer.eon.EonConstant;
import com.exscudo.peer.eon.tx.builders.RegistrationBuilder;
import org.junit.Before;
import org.junit.Test;

public class NoteValidationRuleTest extends AbstractValidationRuleTest {
    private final static String alphabet = "0123456789 abcdefghijklmnoprstuvwxyz ABCDEFGHIJKLMNOPQRSTUVWXYZ #@*-_";
    private NoteValidationRule rule;

    private MockSigner signer = new MockSigner();

    @Override
    protected IValidationRule getValidationRule() {
        return rule;
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        rule = new NoteValidationRule();
    }

    @Test
    public void note_unspecified() throws Exception {
        Transaction tx = RegistrationBuilder.createNew(new byte[32]).build(signer);
        validate(tx);
    }

    @Test
    public void empty_note() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Invalid note.");

        Transaction tx = RegistrationBuilder.createNew(new byte[32]).addNote("").build(signer);
        validate(tx);
    }

    @Test
    public void invalid_note_length() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Invalid note");

        Random random = new Random();
        char[] symbols = alphabet.toCharArray();
        char[] buffer = new char[EonConstant.TRANSACTION_NOTE_MAX_LENGTH + 1];
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = symbols[random.nextInt(symbols.length)];
        }

        Transaction tx = RegistrationBuilder.createNew(new byte[32]).addNote(new String(buffer)).build(signer);
        validate(tx);
    }

    @Test
    public void illegal_symbol() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Invalid note");

        Transaction tx = RegistrationBuilder.createNew(new byte[32]).addNote("%note%").build(signer);
        validate(tx);
    }

    @Test
    public void check_alphabet() throws Exception {
        Transaction tx = RegistrationBuilder.createNew(new byte[32]).addNote(alphabet).build(signer);
        validate(tx);
    }
}
