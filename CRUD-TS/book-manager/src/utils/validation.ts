export interface ValidationError {
  field: string;
  message: string;
}

export function validateBookCreate(body: any): ValidationError[] {
  const errors: ValidationError[] = [];

  if (!body.title || typeof body.title !== "string" || body.title.trim() === "") {
    errors.push({ field: "title", message: "タイトルは必須です" });
  } else if (body.title.length > 200) {
    errors.push({
      field: "title",
      message: "タイトルは200文字以内で入力してください",
    });
  }

  if (!body.author || typeof body.author !== "string" || body.author.trim() === "") {
    errors.push({ field: "author", message: "著者は必須です" });
  } else if (body.author.length > 100) {
    errors.push({
      field: "author",
      message: "著者は100文字以内で入力してください",
    });
  }

  if (body.publisher && body.publisher.length > 100) {
    errors.push({
      field: "publisher",
      message: "出版社は100文字以内で入力してください",
    });
  }

  if (body.publishedDate && !/^\d{4}-\d{2}-\d{2}$/.test(body.publishedDate)) {
    errors.push({
      field: "publishedDate",
      message: "日付はYYYY-MM-DD形式で入力してください",
    });
  }

  if (body.isbn && !/^\d{10}(\d{3})?$/.test(body.isbn)) {
    errors.push({
      field: "isbn",
      message: "ISBNは10桁または13桁の数字で入力してください",
    });
  }

  return errors;
}

export function validateBookUpdate(body: any): ValidationError[] {
  const errors: ValidationError[] = [];

  if (body.title !== undefined) {
    if (typeof body.title !== "string" || body.title.trim() === "") {
      errors.push({ field: "title", message: "タイトルは空にできません" });
    } else if (body.title.length > 200) {
      errors.push({
        field: "title",
        message: "タイトルは200文字以内で入力してください",
      });
    }
  }

  if (body.author !== undefined) {
    if (typeof body.author !== "string" || body.author.trim() === "") {
      errors.push({ field: "author", message: "著者は空にできません" });
    } else if (body.author.length > 100) {
      errors.push({
        field: "author",
        message: "著者は100文字以内で入力してください",
      });
    }
  }

  if (body.publisher !== undefined && body.publisher && body.publisher.length > 100) {
    errors.push({
      field: "publisher",
      message: "出版社は100文字以内で入力してください",
    });
  }

  if (
    body.publishedDate !== undefined &&
    body.publishedDate &&
    !/^\d{4}-\d{2}-\d{2}$/.test(body.publishedDate)
  ) {
    errors.push({
      field: "publishedDate",
      message: "日付はYYYY-MM-DD形式で入力してください",
    });
  }

  if (body.isbn !== undefined && body.isbn && !/^\d{10}(\d{3})?$/.test(body.isbn)) {
    errors.push({
      field: "isbn",
      message: "ISBNは10桁または13桁の数字で入力してください",
    });
  }

  return errors;
}
