package fr.previensmoi.diagnostics
import org.junit.Assert.*
import org.junit.Test
class DiagnosticsTest {
    @Test fun `untrusted request identifier never enters logs`() {
        val value=Diagnostics.format(Diagnostics.Event.Http, Diagnostics.Result.Rejected, -1, 401, "Bearer token email@example.test password=secret")
        assertEquals("event=Http result=Rejected durationMs=0 status=401", value)
    }
    @Test fun `only valid request uuid and status are included`() {
        val id="019a05be-8000-7000-8000-000000000002"
        assertTrue(Diagnostics.format(Diagnostics.Event.Http, Diagnostics.Result.Success, 12, 200, id).endsWith("status=200 requestId=$id"))
        assertFalse(Diagnostics.format(Diagnostics.Event.Http, Diagnostics.Result.Success, 12, 123456).contains("status="))
    }
}
