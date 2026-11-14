package com.fluxisai.proposal;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.math.BigDecimal; import java.util.UUID; import org.junit.jupiter.api.Test;
class ProposalItemTest { @Test void calculatesAmountWithBigDecimal(){var item=new ProposalItem(UUID.randomUUID(),UUID.randomUUID(),"Consultoría",new BigDecimal("2.5"),new BigDecimal("125.50"));assertEquals(0,new BigDecimal("313.750").compareTo(item.getAmount()));} }
