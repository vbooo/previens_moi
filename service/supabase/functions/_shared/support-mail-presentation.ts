import type {SupportMessage} from './support.ts';

/** Display identity only; the client version is not authentication or permission. */
export function supportMailPresentation(message: SupportMessage) {
 const isPhotoMeasures = /^MSP-[a-zA-Z0-9._+-]+-Android-[a-zA-Z0-9._+-]+$/.test(message.appVersion);
 const application = isPhotoMeasures ? 'Mesures sur photos' : 'Préviens-moi';
 const category = message.category === 'problem' ? 'Problème' : message.category === 'idea' ? 'Idée / avis' : 'Question';
 return {
  senderName: `${application} — assistance`,
  subject: `[${application}] ${category}`,
  // Keep legacy mail content unchanged; label the shared service's new client explicitly.
  text: `${message.message}\n\n---\n${isPhotoMeasures ? `Application : ${application}\n` : ''}Version : ${message.appVersion}\nLangue : ${message.language}\nRéférence : ${message.id}`,
 };
}
