import { useCallback, useEffect, useState } from "react";
import { httpsCallable } from "firebase/functions";
import { functions } from "../firebase";
import { quizErrorMessage, quizStrings as s } from "../quizStrings";
import {
  Button,
  Card,
  CommunityIcon,
  ConfirmationDialog,
  EmptyState,
  PageHeader,
} from "./CommunityUi";

export type QuizStatus = "draft" | "ready";
export type QuestionType = "multiple_choice" | "true_false";

export interface QuizSummary {
  quizId: string;
  title: string;
  status: QuizStatus | string;
  questionCount: number;
  updatedAtMs: number;
}

export interface QuizQuestionForm {
  id: string;
  type: QuestionType;
  text: string;
  options: string[];
  correctIndex: number;
  timeLimitSec: number;
}

export interface CommunityQuizAdminProps {
  organizationId: string;
  canManage?: boolean;
  onQuizCountChange?: (count: number) => void;
}

const listQuizzesCallable = httpsCallable<{ organizationId: string }, { quizzes: QuizSummary[] }>(
  functions,
  "listQuizzes",
);

const getQuizCallable = httpsCallable<
  { organizationId: string; quizId: string },
  {
    quizId: string;
    title: string;
    status: QuizStatus;
    questions: Array<{
      type: QuestionType;
      text: string;
      options: string[];
      correctIndex: number;
      timeLimitSec: number;
    }>;
  }
>(functions, "getQuiz");

const saveQuizCallable = httpsCallable<
  {
    organizationId: string;
    quizId?: string | null;
    title: string;
    status: QuizStatus;
    questions: Array<{
      type: QuestionType;
      text: string;
      options: string[];
      correctIndex: number;
      timeLimitSec: number;
    }>;
  },
  { quizId: string; questionCount: number }
>(functions, "saveQuiz");

const deleteQuizCallable = httpsCallable<
  { organizationId: string; quizId: string },
  { quizId: string }
>(functions, "deleteQuiz");

const TIME_LIMIT_OPTIONS = [10, 20, 30, 60] as const;

function generateId(): string {
  return `q_${Date.now()}_${Math.random().toString(36).substring(2, 8)}`;
}

function createDefaultQuestion(): QuizQuestionForm {
  return {
    id: generateId(),
    type: "multiple_choice",
    text: "",
    options: ["", ""],
    correctIndex: 0,
    timeLimitSec: 20,
  };
}

function formatUpdatedDate(ms: number): string {
  if (!ms) return "—";
  return new Intl.DateTimeFormat("tr-TR", {
    dateStyle: "medium",
    timeStyle: "short",
    timeZone: "Europe/Istanbul",
  }).format(new Date(ms));
}

export function CommunityQuizAdmin({
  organizationId,
  canManage = true,
  onQuizCountChange,
}: CommunityQuizAdminProps) {
  const [view, setView] = useState<"list" | "editor">("list");
  const [quizzes, setQuizzes] = useState<QuizSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  // Delete dialog state
  const [deletingQuiz, setDeletingQuiz] = useState<QuizSummary | null>(null);
  const [deleteBusy, setDeleteBusy] = useState(false);

  // Editor form state
  const [editingQuizId, setEditingQuizId] = useState<string | null>(null);
  const [title, setTitle] = useState("");
  const [questions, setQuestions] = useState<QuizQuestionForm[]>([]);

  const loadQuizzes = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const response = await listQuizzesCallable({ organizationId });
      const list = response.data.quizzes ?? [];
      setQuizzes(list);
      onQuizCountChange?.(list.length);
    } catch (requestError) {
      setError(quizErrorMessage(requestError));
    } finally {
      setLoading(false);
    }
  }, [onQuizCountChange, organizationId]);

  useEffect(() => {
    void loadQuizzes();
  }, [loadQuizzes]);

  function startCreate() {
    setError("");
    setNotice("");
    setEditingQuizId(null);
    setTitle("");
    setQuestions([createDefaultQuestion()]);
    setView("editor");
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  async function startEdit(quiz: QuizSummary) {
    setError("");
    setNotice("");
    setLoading(true);
    try {
      const response = await getQuizCallable({ organizationId, quizId: quiz.quizId });
      setEditingQuizId(response.data.quizId);
      setTitle(response.data.title);
      setQuestions(
        response.data.questions.map((q) => ({
          id: generateId(),
          type: q.type,
          text: q.text,
          options: q.type === "true_false" ? ["true", "false"] : [...q.options],
          correctIndex: q.correctIndex,
          timeLimitSec: q.timeLimitSec,
        })),
      );
      setView("editor");
      window.scrollTo({ top: 0, behavior: "smooth" });
    } catch (requestError) {
      setError(quizErrorMessage(requestError));
    } finally {
      setLoading(false);
    }
  }

  function handleCancelEdit() {
    setError("");
    setView("list");
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  // --- Question manipulation helpers ---

  function handleAddQuestion() {
    if (questions.length >= 30) {
      setError(s.maxQuestionsReached);
      return;
    }
    setError("");
    setQuestions((current) => [...current, createDefaultQuestion()]);
  }

  function handleDeleteQuestion(index: number) {
    setError("");
    setQuestions((current) => current.filter((_, i) => i !== index));
  }

  function handleMoveQuestion(index: number, direction: "up" | "down") {
    const targetIndex = direction === "up" ? index - 1 : index + 1;
    if (targetIndex < 0 || targetIndex >= questions.length) return;
    setError("");
    setQuestions((current) => {
      const next = [...current];
      const temp = next[index];
      next[index] = next[targetIndex];
      next[targetIndex] = temp;
      return next;
    });
  }

  function handleTypeChange(index: number, newType: QuestionType) {
    setQuestions((current) => {
      const next = [...current];
      const q = next[index];
      if (q.type === newType) return current;

      if (newType === "true_false") {
        next[index] = {
          ...q,
          type: "true_false",
          options: ["true", "false"],
          correctIndex: q.correctIndex === 1 ? 1 : 0,
        };
      } else {
        next[index] = {
          ...q,
          type: "multiple_choice",
          options: ["", ""],
          correctIndex: 0,
        };
      }
      return next;
    });
  }

  function handleTextChange(index: number, text: string) {
    setQuestions((current) => {
      const next = [...current];
      next[index] = { ...next[index], text };
      return next;
    });
  }

  function handleTimeLimitChange(index: number, timeLimitSec: number) {
    setQuestions((current) => {
      const next = [...current];
      next[index] = { ...next[index], timeLimitSec };
      return next;
    });
  }

  function handleOptionChange(qIndex: number, optIndex: number, value: string) {
    setQuestions((current) => {
      const next = [...current];
      const q = next[qIndex];
      const newOptions = [...q.options];
      newOptions[optIndex] = value;
      next[qIndex] = { ...q, options: newOptions };
      return next;
    });
  }

  function handleCorrectIndexSelect(qIndex: number, correctIndex: number) {
    setQuestions((current) => {
      const next = [...current];
      next[qIndex] = { ...next[qIndex], correctIndex };
      return next;
    });
  }

  function handleAddOption(qIndex: number) {
    setQuestions((current) => {
      const next = [...current];
      const q = next[qIndex];
      if (q.options.length >= 4) return current;
      next[qIndex] = { ...q, options: [...q.options, ""] };
      return next;
    });
  }

  function handleRemoveOption(qIndex: number, optIndex: number) {
    setQuestions((current) => {
      const next = [...current];
      const q = next[qIndex];
      if (q.options.length <= 2) return current;

      const newOptions = q.options.filter((_, i) => i !== optIndex);
      let newCorrectIndex = q.correctIndex;
      if (optIndex === q.correctIndex) {
        newCorrectIndex = Math.min(optIndex, newOptions.length - 1);
      } else if (optIndex < q.correctIndex) {
        newCorrectIndex = q.correctIndex - 1;
      }
      next[qIndex] = {
        ...q,
        options: newOptions,
        correctIndex: newCorrectIndex,
      };
      return next;
    });
  }

  // --- Validation ---

  function validate(targetStatus: QuizStatus): string | null {
    const trimmedTitle = title.trim();
    if (!trimmedTitle) return s.validationTitleRequired;
    if (Array.from(trimmedTitle).length > 80) return s.validationTitleTooLong;

    if (questions.length > 30) return s.validationMaxQuestions;

    if (targetStatus === "ready" && questions.length === 0) {
      return s.validationQuestionsRequiredForReady;
    }

    for (let i = 0; i < questions.length; i++) {
      const q = questions[i];
      const trimmedText = q.text.trim();
      if (!trimmedText) return s.validationQuestionTextRequired(i + 1);
      if (Array.from(trimmedText).length > 120) return s.validationQuestionTextTooLong(i + 1);

      if (q.type === "multiple_choice") {
        if (q.options.length < 2 || q.options.length > 4) {
          return s.validationOptionsMin(i + 1);
        }
        const cleaned: string[] = [];
        for (let j = 0; j < q.options.length; j++) {
          const opt = q.options[j].trim();
          if (!opt) return s.validationOptionEmpty(i, j);
          if (Array.from(opt).length > 75) return s.validationOptionTooLong(i, j);
          cleaned.push(opt);
        }
        const set = new Set(cleaned.map((o) => o.toLocaleLowerCase("tr-TR")));
        if (set.size !== cleaned.length) {
          return s.validationOptionsDuplicate(i + 1);
        }
        if (q.correctIndex < 0 || q.correctIndex >= q.options.length) {
          return s.validationCorrectIndexInvalid(i + 1);
        }
      } else if (q.type === "true_false") {
        if (q.correctIndex !== 0 && q.correctIndex !== 1) {
          return s.validationCorrectIndexInvalid(i + 1);
        }
      }
    }

    return null;
  }

  // --- Save / Delete actions ---

  async function handleSave(status: QuizStatus) {
    const validationError = validate(status);
    if (validationError) {
      setError(validationError);
      window.scrollTo({ top: 0, behavior: "smooth" });
      return;
    }

    setSaving(true);
    setError("");
    setNotice("");

    const payloadQuestions = questions.map((q) => ({
      type: q.type,
      text: q.text.trim(),
      options:
        q.type === "true_false"
          ? ["true", "false"]
          : q.options.map((opt) => opt.trim()),
      correctIndex: q.correctIndex,
      timeLimitSec: q.timeLimitSec,
    }));

    try {
      await saveQuizCallable({
        organizationId,
        quizId: editingQuizId || null,
        title: title.trim(),
        status,
        questions: payloadQuestions,
      });

      setNotice(s.saveSuccessNotice(status));
      setView("list");
      await loadQuizzes();
      window.scrollTo({ top: 0, behavior: "smooth" });
    } catch (requestError) {
      setError(quizErrorMessage(requestError));
      window.scrollTo({ top: 0, behavior: "smooth" });
    } finally {
      setSaving(false);
    }
  }

  async function confirmDelete() {
    if (!deletingQuiz) return;
    setDeleteBusy(true);
    setError("");
    setNotice("");
    try {
      await deleteQuizCallable({
        organizationId,
        quizId: deletingQuiz.quizId,
      });
      setNotice(s.deleteSuccessNotice);
      setDeletingQuiz(null);
      await loadQuizzes();
    } catch (requestError) {
      setError(quizErrorMessage(requestError));
    } finally {
      setDeleteBusy(false);
    }
  }

  // --- Render ---

  return (
    <div className="quiz-admin">
      {error ? (
        <div className="inline-message inline-message--error admin-message" role="alert">
          {error}
        </div>
      ) : null}
      {notice ? (
        <div className="inline-message inline-message--success admin-message" role="status">
          {notice}
        </div>
      ) : null}

      {view === "list" ? (
        <>
          <PageHeader
            title={s.pageTitle}
            description={s.pageDescription}
            action={
              <Button
                variant="primary"
                icon="plus"
                disabled={!canManage || quizzes.length >= 20}
                onClick={startCreate}
              >
                {s.newQuizButton}
              </Button>
            }
          />

          {loading ? (
            <Card className="admin-loading">
              <span className="spinner spinner--green" />
              <p>{s.loading}</p>
            </Card>
          ) : quizzes.length === 0 ? (
            <EmptyState
              title={s.emptyTitle}
              description={s.emptyDescription}
              action={
                <Button
                  variant="primary"
                  icon="plus"
                  disabled={!canManage}
                  onClick={startCreate}
                >
                  {s.emptyAction}
                </Button>
              }
            />
          ) : (
            <Card className="quiz-table-container">
              <div className="community-section-heading" style={{ padding: "16px 18px 0" }}>
                <div>
                  <h2 id="quiz-list-title">{s.listTitle}</h2>
                  <p>{s.listDescription}</p>
                </div>
                <Button variant="ghost" onClick={() => void loadQuizzes()} disabled={loading}>
                  {s.refreshButton}
                </Button>
              </div>

              <table className="quiz-table" aria-labelledby="quiz-list-title">
                <thead>
                  <tr>
                    <th>{s.tableHeaderTitle}</th>
                    <th>{s.tableHeaderQuestions}</th>
                    <th>{s.tableHeaderStatus}</th>
                    <th>{s.tableHeaderUpdated}</th>
                    <th style={{ textAlign: "right" }}>{s.tableHeaderActions}</th>
                  </tr>
                </thead>
                <tbody>
                  {quizzes.map((quiz) => (
                    <tr key={quiz.quizId}>
                      <td className="quiz-title-cell">{quiz.title}</td>
                      <td>{quiz.questionCount} soru</td>
                      <td>
                        <span
                          className={`ui-badge ${
                            quiz.status === "ready" ? "ui-badge--published" : "ui-badge--pending"
                          }`}
                        >
                          {quiz.status === "ready" ? s.statusReady : s.statusDraft}
                        </span>
                      </td>
                      <td>{formatUpdatedDate(quiz.updatedAtMs)}</td>
                      <td>
                        <div className="quiz-actions-cell">
                          <Button
                            variant="secondary"
                            disabled={!canManage}
                            onClick={() => void startEdit(quiz)}
                          >
                            {s.editButton}
                          </Button>
                          <Button
                            variant="danger"
                            disabled={!canManage}
                            onClick={() => setDeletingQuiz(quiz)}
                          >
                            {s.deleteButton}
                          </Button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </Card>
          )}
        </>
      ) : (
        /* --- Editor Mode --- */
        <div className="quiz-editor">
          <PageHeader
            title={editingQuizId ? s.editHeading : s.createHeading}
            description={s.editorHint}
            action={
              <Button variant="ghost" onClick={handleCancelEdit}>
                {s.backToList}
              </Button>
            }
          />

          {/* Quiz Top Info Card */}
          <section className="quiz-top-card">
            <div className="quiz-field-group">
              <div className="quiz-field-header">
                <label className="field-label" htmlFor="quiz-title">
                  {s.titleLabel} *
                </label>
                <span className="quiz-char-counter">
                  {Array.from(title).length} / 80
                </span>
              </div>
              <input
                id="quiz-title"
                type="text"
                maxLength={80}
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                placeholder={s.titlePlaceholder}
                required
              />
            </div>
          </section>

          {/* Questions Section */}
          <div className="quiz-section-header">
            <h2>{s.questionsHeading}</h2>
            <span className="quiz-count-pill">{s.questionsCount(questions.length, 30)}</span>
          </div>

          {questions.length === 0 ? (
            <Card style={{ padding: "32px", textAlign: "center" }}>
              <p style={{ color: "var(--text-secondary)", margin: "0 0 16px" }}>{s.noQuestionsAdded}</p>
              <Button variant="primary" icon="plus" onClick={handleAddQuestion}>
                {s.addQuestionButton}
              </Button>
            </Card>
          ) : (
            questions.map((q, qIndex) => (
              <article className="quiz-question-card" key={q.id}>
                {/* Question Header */}
                <div className="quiz-question-top">
                  <div className="quiz-question-title">
                    <span className="quiz-question-badge">{qIndex + 1}</span>
                    <strong>{s.questionNumber(qIndex + 1)}</strong>
                    <span className="quiz-question-type-badge">
                      {q.type === "true_false" ? s.typeTrueFalse : s.typeMultipleChoice}
                    </span>
                  </div>

                  <div className="quiz-question-actions">
                    <button
                      type="button"
                      className="quiz-icon-btn"
                      disabled={qIndex === 0}
                      onClick={() => handleMoveQuestion(qIndex, "up")}
                      title={s.moveUp}
                      aria-label={`${s.questionNumber(qIndex + 1)} ${s.moveUp}`}
                    >
                      <CommunityIcon name="arrowUp" />
                    </button>
                    <button
                      type="button"
                      className="quiz-icon-btn"
                      disabled={qIndex === questions.length - 1}
                      onClick={() => handleMoveQuestion(qIndex, "down")}
                      title={s.moveDown}
                      aria-label={`${s.questionNumber(qIndex + 1)} ${s.moveDown}`}
                    >
                      <CommunityIcon name="arrowDown" />
                    </button>
                    <button
                      type="button"
                      className="quiz-icon-btn quiz-icon-btn--danger"
                      onClick={() => handleDeleteQuestion(qIndex)}
                      title={s.deleteQuestion}
                      aria-label={`${s.questionNumber(qIndex + 1)} ${s.deleteQuestion}`}
                    >
                      <CommunityIcon name="trash" />
                    </button>
                  </div>
                </div>

                {/* Question Text */}
                <div className="quiz-field-group">
                  <div className="quiz-field-header">
                    <label className="field-label" htmlFor={`question-text-${q.id}`}>
                      {s.questionTextLabel} *
                    </label>
                    <span className="quiz-char-counter">
                      {Array.from(q.text).length} / 120
                    </span>
                  </div>
                  <textarea
                    id={`question-text-${q.id}`}
                    maxLength={120}
                    rows={2}
                    value={q.text}
                    onChange={(e) => handleTextChange(qIndex, e.target.value)}
                    placeholder={s.questionTextPlaceholder}
                    required
                  />
                </div>

                {/* Type & Time Limit Controls */}
                <div className="quiz-two-col">
                  <div className="quiz-field-group">
                    <label className="field-label" htmlFor={`question-type-${q.id}`}>
                      {s.questionTypeLabel}
                    </label>
                    <select
                      id={`question-type-${q.id}`}
                      value={q.type}
                      onChange={(e) => handleTypeChange(qIndex, e.target.value as QuestionType)}
                    >
                      <option value="multiple_choice">{s.typeMultipleChoice}</option>
                      <option value="true_false">{s.typeTrueFalse}</option>
                    </select>
                  </div>

                  <div className="quiz-field-group">
                    <label className="field-label" htmlFor={`question-time-${q.id}`}>
                      {s.timeLimitLabel}
                    </label>
                    <select
                      id={`question-time-${q.id}`}
                      value={q.timeLimitSec}
                      onChange={(e) => handleTimeLimitChange(qIndex, Number(e.target.value))}
                    >
                      {TIME_LIMIT_OPTIONS.map((sec) => (
                        <option key={sec} value={sec}>
                          {s.timeLimitOption(sec)}
                        </option>
                      ))}
                    </select>
                  </div>
                </div>

                {/* Options Area */}
                <div className="quiz-field-group">
                  <div className="quiz-field-header">
                    <span className="field-label">{s.optionsHeading}</span>
                    <small style={{ color: "var(--text-secondary)" }}>{s.optionsHelp}</small>
                  </div>

                  {q.type === "multiple_choice" ? (
                    <div className="quiz-options-wrapper">
                      {q.options.map((opt, optIndex) => {
                        const isCorrect = q.correctIndex === optIndex;
                        return (
                          <div
                            key={optIndex}
                            className={`quiz-option-item ${isCorrect ? "is-correct" : ""}`}
                          >
                            <input
                              type="radio"
                              name={`correct-answer-${q.id}`}
                              className="quiz-radio"
                              checked={isCorrect}
                              onChange={() => handleCorrectIndexSelect(qIndex, optIndex)}
                              title={s.correctOptionRadio}
                              aria-label={`${s.questionNumber(qIndex + 1)} - ${s.optionPlaceholder(optIndex)} ${s.correctOptionRadio}`}
                            />

                            <div className="quiz-option-input-wrapper">
                              <input
                                type="text"
                                maxLength={75}
                                value={opt}
                                onChange={(e) => handleOptionChange(qIndex, optIndex, e.target.value)}
                                placeholder={s.optionPlaceholder(optIndex)}
                                required
                              />
                              <span className="quiz-option-counter">
                                {Array.from(opt).length}/75
                              </span>
                            </div>

                            {isCorrect ? (
                              <span className="quiz-correct-indicator">{s.correctBadge}</span>
                            ) : null}

                            {q.options.length > 2 ? (
                              <button
                                type="button"
                                className="quiz-icon-btn quiz-icon-btn--danger"
                                onClick={() => handleRemoveOption(qIndex, optIndex)}
                                title={s.removeOption}
                                aria-label={`${s.optionPlaceholder(optIndex)} ${s.removeOption}`}
                              >
                                <CommunityIcon name="close" />
                              </button>
                            ) : null}
                          </div>
                        );
                      })}

                      {q.options.length < 4 ? (
                        <div style={{ marginTop: "4px" }}>
                          <Button
                            type="button"
                            variant="ghost"
                            icon="plus"
                            onClick={() => handleAddOption(qIndex)}
                          >
                            {s.addOptionButton}
                          </Button>
                        </div>
                      ) : null}
                    </div>
                  ) : (
                    /* True / False Options */
                    <div className="quiz-tf-group">
                      <div
                        className={`quiz-tf-card ${q.correctIndex === 0 ? "is-selected" : ""}`}
                        onClick={() => handleCorrectIndexSelect(qIndex, 0)}
                        role="button"
                        tabIndex={0}
                        onKeyDown={(e) => {
                          if (e.key === " " || e.key === "Enter") handleCorrectIndexSelect(qIndex, 0);
                        }}
                      >
                        <input
                          type="radio"
                          name={`correct-answer-${q.id}`}
                          className="quiz-radio"
                          checked={q.correctIndex === 0}
                          onChange={() => handleCorrectIndexSelect(qIndex, 0)}
                          aria-label={`${s.questionNumber(qIndex + 1)}: ${s.trueOptionLabel}`}
                        />
                        <span>{s.trueOptionLabel}</span>
                        {q.correctIndex === 0 ? (
                          <span className="quiz-correct-indicator" style={{ marginLeft: "auto" }}>
                            {s.correctBadge}
                          </span>
                        ) : null}
                      </div>

                      <div
                        className={`quiz-tf-card ${q.correctIndex === 1 ? "is-selected" : ""}`}
                        onClick={() => handleCorrectIndexSelect(qIndex, 1)}
                        role="button"
                        tabIndex={0}
                        onKeyDown={(e) => {
                          if (e.key === " " || e.key === "Enter") handleCorrectIndexSelect(qIndex, 1);
                        }}
                      >
                        <input
                          type="radio"
                          name={`correct-answer-${q.id}`}
                          className="quiz-radio"
                          checked={q.correctIndex === 1}
                          onChange={() => handleCorrectIndexSelect(qIndex, 1)}
                          aria-label={`${s.questionNumber(qIndex + 1)}: ${s.falseOptionLabel}`}
                        />
                        <span>{s.falseOptionLabel}</span>
                        {q.correctIndex === 1 ? (
                          <span className="quiz-correct-indicator" style={{ marginLeft: "auto" }}>
                            {s.correctBadge}
                          </span>
                        ) : null}
                      </div>
                    </div>
                  )}
                </div>
              </article>
            ))
          )}

          {/* Add Question Button Card */}
          {questions.length < 30 ? (
            <div className="quiz-add-question-card">
              <Button type="button" variant="secondary" icon="plus" onClick={handleAddQuestion}>
                {s.addQuestionButton}
              </Button>
            </div>
          ) : null}

          {/* Sticky Editor Actions */}
          <div className="quiz-editor-actions">
            <Button type="button" variant="ghost" disabled={saving} onClick={handleCancelEdit}>
              {s.cancelButton}
            </Button>
            <Button
              type="button"
              variant="secondary"
              disabled={saving || !canManage}
              onClick={() => void handleSave("draft")}
            >
              {saving ? s.saving : s.saveDraftButton}
            </Button>
            <Button
              type="button"
              variant="primary"
              disabled={saving || !canManage}
              onClick={() => void handleSave("ready")}
            >
              {saving ? s.saving : s.saveReadyButton}
            </Button>
          </div>
        </div>
      )}

      {/* Confirmation Dialog for Deletion */}
      <ConfirmationDialog
        open={Boolean(deletingQuiz)}
        title={s.deleteDialogTitle}
        description={deletingQuiz ? s.deleteDialogDescription(deletingQuiz.title) : ""}
        confirmLabel={s.deleteConfirm}
        busy={deleteBusy}
        onClose={() => setDeletingQuiz(null)}
        onConfirm={() => void confirmDelete()}
      />
    </div>
  );
}
