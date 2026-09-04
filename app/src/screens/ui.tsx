import type { ReactNode } from "react";

export function Modal({
  title,
  onClose,
  children,
  width = 460,
}: {
  title: string;
  onClose: () => void;
  children: ReactNode;
  width?: number;
}) {
  return (
    <div className="ui-modal-backdrop" onClick={onClose} role="presentation">
      <div className="ui-modal" style={{ maxWidth: width }} onClick={(e) => e.stopPropagation()}>
        <style>{modalCss}</style>
        <div className="ui-modal-head">
          <h2>{title}</h2>
          <button type="button" onClick={onClose} aria-label="Cerrar">
            ×
          </button>
        </div>
        <div className="ui-modal-body">{children}</div>
      </div>
    </div>
  );
}

const modalCss = `
.ui-modal-backdrop{position:fixed;inset:0;background:rgba(22,19,17,.55);display:flex;align-items:center;
  justify-content:center;padding:32px;z-index:100}
.ui-modal{background:#FEFDFA;border:1px solid #E5E0D2;border-radius:16px;width:100%;max-height:82vh;
  display:flex;flex-direction:column;box-shadow:0 30px 60px -30px rgba(36,33,26,.6);
  font-family:'Inter',ui-sans-serif,system-ui,sans-serif;color:#24211A}
.ui-modal *{box-sizing:border-box}
.ui-modal-head{display:flex;align-items:center;justify-content:space-between;gap:12px;padding:16px 20px;
  border-bottom:1px solid #E5E0D2}
.ui-modal-head h2{font-family:'Fraunces',Georgia,serif;font-size:19px;font-weight:600;margin:0}
.ui-modal-head button{border:none;background:none;font-size:22px;line-height:1;color:#726C5C;cursor:pointer;padding:0 4px}
.ui-modal-body{padding:18px 20px 22px;overflow-y:auto}
`;
