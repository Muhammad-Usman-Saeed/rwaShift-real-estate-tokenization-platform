"use client";

import { useRef, useState, type DragEvent } from "react";
import { cn } from "@/lib/utils/cn";
import { Upload, FileText, X } from "@/components/ui/icons";

export function FileUpload({
  onFilesSelected,
  accept,
  multiple = false,
  selectedFiles = [],
  onRemove,
  id,
}: {
  onFilesSelected: (files: File[]) => void;
  accept?: string;
  multiple?: boolean;
  selectedFiles?: File[];
  onRemove?: (index: number) => void;
  id?: string;
}) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [isDragOver, setIsDragOver] = useState(false);

  function handleDrop(e: DragEvent<HTMLDivElement>) {
    e.preventDefault();
    setIsDragOver(false);
    if (e.dataTransfer.files.length) onFilesSelected(Array.from(e.dataTransfer.files));
  }

  return (
    <div className="flex flex-col gap-2">
      <div
        role="button"
        tabIndex={0}
        onClick={() => inputRef.current?.click()}
        onKeyDown={(e) => (e.key === "Enter" || e.key === " ") && inputRef.current?.click()}
        onDragOver={(e) => {
          e.preventDefault();
          setIsDragOver(true);
        }}
        onDragLeave={() => setIsDragOver(false)}
        onDrop={handleDrop}
        className={cn(
          "flex cursor-pointer flex-col items-center justify-center gap-2 rounded-md border-2 border-dashed px-6 py-8 text-center transition-colors",
          isDragOver ? "border-brand-500 bg-brand-50" : "border-surface-border bg-surface-subtle hover:border-brand-300",
        )}
      >
        <Upload className="h-5 w-5 text-ink-400" />
        <p className="text-sm text-ink-600">
          <span className="font-medium text-brand-700">Click to upload</span> or drag and drop
        </p>
        <input
          ref={inputRef}
          id={id}
          type="file"
          accept={accept}
          multiple={multiple}
          className="sr-only"
          onChange={(e) => e.target.files && onFilesSelected(Array.from(e.target.files))}
        />
      </div>
      {selectedFiles.length > 0 && (
        <ul className="flex flex-col gap-1.5">
          {selectedFiles.map((file, index) => (
            <li
              key={`${file.name}-${index}`}
              className="flex items-center justify-between gap-2 rounded-md border border-surface-border bg-white px-3 py-2 text-sm"
            >
              <span className="flex items-center gap-2 truncate text-ink-700">
                <FileText className="h-4 w-4 shrink-0 text-ink-400" />
                <span className="truncate">{file.name}</span>
              </span>
              {onRemove && (
                <button
                  type="button"
                  onClick={() => onRemove(index)}
                  className="shrink-0 text-ink-400 hover:text-danger-600"
                  aria-label={`Remove ${file.name}`}
                >
                  <X className="h-4 w-4" />
                </button>
              )}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
