import { apiClient } from "../../shared/api/apiClient";
import type {
  CheckInTicketResponse,
  VerifyTicketResponse,
} from "../../types/ticket";

export async function verifyTicket(qrCode: string) {
  const encodedQrCode = encodeURIComponent(qrCode);

  const response = await apiClient.get<VerifyTicketResponse>(
    `/tickets/verify/${encodedQrCode}`,
  );

  return response.data;
}

export async function checkInTicket(ticketId: string) {
  const response = await apiClient.post<CheckInTicketResponse>(
    `/tickets/${ticketId}/check-in`,
  );

  return response.data;
}